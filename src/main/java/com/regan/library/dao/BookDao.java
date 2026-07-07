package com.regan.library.dao;

import com.regan.library.config.Db;
import com.regan.library.exception.LibraryException;
import com.regan.library.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Read/write access to the BOOKS table (plain JDBC). */
public class BookDao {

    private static final String BASE_SELECT =
        "SELECT book_id, title, author, isbn, published_year, total_copies, available_copies FROM books";

    public List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        try (Connection c = Db.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(BASE_SELECT + " ORDER BY title")) {
            while (rs.next()) books.add(map(rs));
        } catch (SQLException e) {
            throw new LibraryException("Failed to list books", e);
        }
        return books;
    }

    public List<Book> searchByTitle(String term) {
        List<Book> books = new ArrayList<>();
        String sql = BASE_SELECT + " WHERE UPPER(title) LIKE UPPER(?) ORDER BY title";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + term + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) books.add(map(rs));
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to search books", e);
        }
        return books;
    }

    public Optional<Book> findById(long id) {
        String sql = BASE_SELECT + " WHERE book_id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to load book " + id, e);
        }
    }

    /** Inserts a new title; returns the generated BOOK_ID. */
    public long insert(String title, String author, String isbn, Integer year, int copies) {
        String sql = "INSERT INTO books (title, author, isbn, published_year, total_copies, available_copies) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[] {"BOOK_ID"})) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, isbn);
            if (year == null) ps.setNull(4, java.sql.Types.NUMERIC); else ps.setInt(4, year);
            ps.setInt(5, copies);
            ps.setInt(6, copies);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed to add book (duplicate ISBN?)", e);
        }
    }

    private Book map(ResultSet rs) throws SQLException {
        int year = rs.getInt("published_year");
        return new Book(
                rs.getLong("book_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("isbn"),
                rs.wasNull() ? null : year,
                rs.getInt("total_copies"),
                rs.getInt("available_copies"));
    }
}
