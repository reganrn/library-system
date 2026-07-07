package com.regan.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A borrow record; open while returnDate is null. */
public class Loan {

    private final long id;
    private final long bookId;
    private final long memberId;
    private final String bookTitle;    // joined in for display
    private final String memberName;   // joined in for display
    private final LocalDate loanDate;
    private final LocalDate dueDate;
    private final LocalDate returnDate; // nullable
    private final BigDecimal fine;

    public Loan(long id, long bookId, long memberId, String bookTitle, String memberName,
                LocalDate loanDate, LocalDate dueDate, LocalDate returnDate, BigDecimal fine) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.bookTitle = bookTitle;
        this.memberName = memberName;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fine = fine;
    }

    public long getId()             { return id; }
    public long getBookId()         { return bookId; }
    public long getMemberId()       { return memberId; }
    public String getBookTitle()    { return bookTitle; }
    public String getMemberName()   { return memberName; }
    public LocalDate getLoanDate()  { return loanDate; }
    public LocalDate getDueDate()   { return dueDate; }
    public LocalDate getReturnDate(){ return returnDate; }
    public BigDecimal getFine()     { return fine; }
    public boolean isOpen()         { return returnDate == null; }
    public boolean isOverdue()      { return isOpen() && LocalDate.now().isAfter(dueDate); }

    @Override
    public String toString() {
        String state = returnDate != null
                ? "returned " + returnDate + (fine.signum() > 0 ? " fine " + fine : "")
                : (isOverdue() ? "OVERDUE (due " + dueDate + ")" : "due " + dueDate);
        return String.format("[%d] \"%s\" -> %s | borrowed %s | %s",
                id, bookTitle, memberName, loanDate, state);
    }
}
