package com.regan.library.exception;

/**
 * Business-rule violation surfaced from the database layer.
 * Maps ORA-20001..20005 raised by LIBRARY_PKG into readable messages.
 */
public class LibraryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int oraCode; // e.g. 20002, or 0 if not a business error

    public LibraryException(String message, int oraCode, Throwable cause) {
        super(message, cause);
        this.oraCode = oraCode;
    }

    public LibraryException(String message, Throwable cause) {
        this(message, 0, cause);
    }

    public int getOraCode() { return oraCode; }
}
