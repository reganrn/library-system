package com.regan.library.ui;

import com.regan.library.dao.LoanDao;
import com.regan.library.exception.LibraryException;
import com.regan.library.model.Book;
import com.regan.library.model.Loan;
import com.regan.library.model.Member;
import com.regan.library.service.LibraryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

// Interactive menu. All input parsing lives here; no SQL beyond this point.
public class ConsoleUI {

    private final LibraryService service = new LibraryService();
    private final Scanner in = new Scanner(System.in);

    public void run() {
        System.out.println("=== Library Management System ===");
        while (true) {
            printMenu();
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> listBooks();
                    case "2" -> searchBooks();
                    case "3" -> addBook();
                    case "4" -> listMembers();
                    case "5" -> registerMember();
                    case "6" -> borrowBook();
                    case "7" -> returnBook();
                    case "8" -> showLoans(service.openLoans(), "Open loans");
                    case "9" -> showLoans(service.loanHistory(), "Loan history");
                    case "0" -> { System.out.println("Bye."); return; }
                    default  -> System.out.println("Unknown option.");
                }
            } catch (LibraryException e) {
                System.out.println("!! " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("!! Please enter a valid number.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1) List books        4) List members     7) Return a book");
        System.out.println("2) Search books      5) Register member  8) Open loans");
        System.out.println("3) Add book          6) Borrow a book    9) Loan history");
        System.out.println("0) Exit");
        System.out.print("> ");
    }

    private void listBooks() {
        List<Book> books = service.listBooks();
        if (books.isEmpty()) System.out.println("(no books)");
        books.forEach(System.out::println);
    }

    private void searchBooks() {
        System.out.print("Title contains: ");
        List<Book> books = service.searchBooks(in.nextLine());
        if (books.isEmpty()) System.out.println("(no matches)");
        books.forEach(System.out::println);
    }

    private void addBook() {
        System.out.print("Title: ");
        String title = in.nextLine();
        System.out.print("Author: ");
        String author = in.nextLine();
        System.out.print("ISBN: ");
        String isbn = in.nextLine();
        System.out.print("Published year (blank if unknown): ");
        String yearStr = in.nextLine().trim();
        Integer year = yearStr.isEmpty() ? null : Integer.valueOf(yearStr);
        System.out.print("Number of copies: ");
        int copies = Integer.parseInt(in.nextLine().trim());
        long id = service.addBook(title, author, isbn, year, copies);
        System.out.println("Added book #" + id);
    }

    private void listMembers() {
        List<Member> members = service.listMembers();
        if (members.isEmpty()) System.out.println("(no members)");
        members.forEach(System.out::println);
    }

    private void registerMember() {
        System.out.print("Full name: ");
        String name = in.nextLine();
        System.out.print("Email: ");
        String email = in.nextLine();
        long id = service.registerMember(name, email);
        System.out.println("Registered member #" + id);
    }

    private void borrowBook() {
        System.out.print("Book id: ");
        long bookId = Long.parseLong(in.nextLine().trim());
        System.out.print("Member id: ");
        long memberId = Long.parseLong(in.nextLine().trim());
        LoanDao.BorrowResult r = service.borrow(bookId, memberId);
        System.out.println("Loan #" + r.loanId() + " created, due " + r.dueDate());
    }

    private void returnBook() {
        System.out.print("Loan id: ");
        long loanId = Long.parseLong(in.nextLine().trim());
        BigDecimal fine = service.returnLoan(loanId);
        System.out.println(fine.signum() > 0
                ? "Returned late - fine: " + fine
                : "Returned on time. No fine.");
    }

    private void showLoans(List<Loan> loans, String header) {
        System.out.println("-- " + header + " --");
        if (loans.isEmpty()) System.out.println("(none)");
        loans.forEach(System.out::println);
    }
}
