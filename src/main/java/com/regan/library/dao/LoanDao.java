package com.regan.library.dao;

import com.regan.library.config.Db;
import com.regan.library.exception.LibraryException;
import com.regan.library.model.Loan;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Loan operations. Borrow/return are delegated to the LIBRARY_PKG PL/SQL
 * package via CallableStatement, so the transactional business rules run
 * inside the database in a single round trip.
 */
public class LoanDao {

    /** Result of a successful borrow. */
    public record BorrowResult(long loanId, LocalDate dueDate) {}

    public BorrowResult borrow(long bookId, long memberId) {
        String call = "{call library_pkg.borrow_book(?, ?, ?, ?)}";
        try (Connection c = Db.getConnection();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setLong(1, bookId);
            cs.setLong(2, memberId);
            cs.registerOutParameter(3, Types.NUMERIC); // p_loan_id
            cs.registerOutParameter(4, Types.DATE);    // p_due_date
            cs.execute();
            return new BorrowResult(cs.getLong(3), cs.getDate(4).toLocalDate());
        } catch (SQLException e) {
            throw translate(e, "Borrow failed");
        }
    }

    /** Returns the fine charged (0 if on time). */
    public BigDecimal returnLoan(long loanId) {
        String call = "{call library_pkg.return_book(?, ?)}";
        try (Connection c = Db.getConnection();
             CallableStatement cs = c.prepareCall(call)) {
            cs.setLong(1, loanId);
            cs.registerOutParameter(2, Types.NUMERIC); // p_fine
            cs.execute();
            BigDecimal fine = cs.getBigDecimal(2);
            return fine != null ? fine : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw translate(e, "Return failed");
        }
    }

    public List<Loan> findOpenLoans() {
        String sql =
            "SELECT l.loan_id, l.book_id, l.member_id, b.title, m.full_name, " +
            "       l.loan_date, l.due_date, l.return_date, l.fine " +
            "FROM   loans l " +
            "JOIN   books b   ON b.book_id   = l.book_id " +
            "JOIN   members m ON m.member_id = l.member_id " +
            "WHERE  l.return_date IS NULL " +
            "ORDER  BY l.due_date";
        return query(sql);
    }

    public List<Loan> findHistory() {
        String sql =
            "SELECT l.loan_id, l.book_id, l.member_id, b.title, m.full_name, " +
            "       l.loan_date, l.due_date, l.return_date, l.fine " +
            "FROM   loans l " +
            "JOIN   books b   ON b.book_id   = l.book_id " +
            "JOIN   members m ON m.member_id = l.member_id " +
            "ORDER  BY l.loan_date DESC";
        return query(sql);
    }

    private List<Loan> query(String sql) {
        List<Loan> loans = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) loans.add(map(rs));
        } catch (SQLException e) {
            throw new LibraryException("Failed to list loans", e);
        }
        return loans;
    }

    private Loan map(ResultSet rs) throws SQLException {
        Date ret = rs.getDate("return_date");
        return new Loan(
                rs.getLong("loan_id"),
                rs.getLong("book_id"),
                rs.getLong("member_id"),
                rs.getString("title"),
                rs.getString("full_name"),
                rs.getDate("loan_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                ret != null ? ret.toLocalDate() : null,
                rs.getBigDecimal("fine"));
    }

    /**
     * Maps ORA-20001..20005 (raised by LIBRARY_PKG) to clean business
     * messages; anything else is wrapped as a technical failure.
     */
    private LibraryException translate(SQLException e, String action) {
        int code = e.getErrorCode();
        if (code >= 20001 && code <= 20005) {
            // Oracle prefixes messages with "ORA-2000x: " - strip it for the user
            String msg = e.getMessage();
            int idx = msg.indexOf(':');
            if (idx > 0 && msg.startsWith("ORA-")) {
                msg = msg.substring(idx + 1).trim();
                int nl = msg.indexOf('\n');
                if (nl > 0) msg = msg.substring(0, nl);
            }
            return new LibraryException(msg, code, e);
        }
        return new LibraryException(action + " (technical error)", e);
    }
}
