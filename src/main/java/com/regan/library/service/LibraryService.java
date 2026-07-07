package com.regan.library.service;

import com.regan.library.dao.BookDao;
import com.regan.library.dao.LoanDao;
import com.regan.library.dao.MemberDao;
import com.regan.library.exception.LibraryException;
import com.regan.library.model.Book;
import com.regan.library.model.Loan;
import com.regan.library.model.Member;

import java.math.BigDecimal;
import java.util.List;

/**
 * Application service - the single entry point the UI talks to.
 * Validation of *input shape* happens here; *business rules*
 * (availability, loan limits, fines) are enforced in LIBRARY_PKG.
 */
public class LibraryService {

    private final BookDao bookDao = new BookDao();
    private final MemberDao memberDao = new MemberDao();
    private final LoanDao loanDao = new LoanDao();

    // catalogue
    public List<Book> listBooks()               { return bookDao.findAll(); }
    public List<Book> searchBooks(String term)  { return bookDao.searchByTitle(require(term, "search term")); }

    public long addBook(String title, String author, String isbn, Integer year, int copies) {
        if (copies < 1) throw new LibraryException("Copies must be at least 1", null);
        return bookDao.insert(require(title, "title"), require(author, "author"),
                              require(isbn, "ISBN"), year, copies);
    }

    // members
    public List<Member> listMembers() { return memberDao.findAll(); }

    public long registerMember(String fullName, String email) {
        String mail = require(email, "email");
        if (!mail.contains("@")) throw new LibraryException("Invalid email address", null);
        return memberDao.insert(require(fullName, "name"), mail);
    }

    // loans
    public LoanDao.BorrowResult borrow(long bookId, long memberId) {
        return loanDao.borrow(bookId, memberId);
    }

    public BigDecimal returnLoan(long loanId) {
        return loanDao.returnLoan(loanId);
    }

    public List<Loan> openLoans()   { return loanDao.findOpenLoans(); }
    public List<Loan> loanHistory() { return loanDao.findHistory(); }

    private String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new LibraryException("Missing " + field, null);
        }
        return value.trim();
    }
}
