package com.regan.library.model;

/** A book title in the catalogue (one row may represent several physical copies). */
public class Book {

    private final long id;
    private final String title;
    private final String author;
    private final String isbn;
    private final Integer publishedYear;
    private final int totalCopies;
    private final int availableCopies;

    public Book(long id, String title, String author, String isbn,
                Integer publishedYear, int totalCopies, int availableCopies) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publishedYear = publishedYear;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public long getId()              { return id; }
    public String getTitle()         { return title; }
    public String getAuthor()        { return author; }
    public String getIsbn()          { return isbn; }
    public Integer getPublishedYear(){ return publishedYear; }
    public int getTotalCopies()      { return totalCopies; }
    public int getAvailableCopies()  { return availableCopies; }

    @Override
    public String toString() {
        return String.format("[%d] %s — %s (%s) %d/%d available",
                id, title, author, isbn, availableCopies, totalCopies);
    }
}
