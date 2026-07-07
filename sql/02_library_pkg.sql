-- LIBRARY_PKG - all business logic lives here, close to the data.
--
-- Error codes (raised with RAISE_APPLICATION_ERROR, caught by the Java layer):
--   -20001  book not found
--   -20002  no copies available
--   -20003  member not found or inactive
--   -20004  member has reached the active-loan limit
--   -20005  loan not found or already returned

CREATE OR REPLACE PACKAGE library_pkg AS

  c_loan_days    CONSTANT PLS_INTEGER := 14;   -- loan period
  c_max_loans    CONSTANT PLS_INTEGER := 3;    -- active loans per member
  c_fine_per_day CONSTANT NUMBER(4,2) := 0.50; -- currency units per late day

  PROCEDURE borrow_book (
    p_book_id   IN  loans.book_id%TYPE,
    p_member_id IN  loans.member_id%TYPE,
    p_loan_id   OUT loans.loan_id%TYPE,
    p_due_date  OUT loans.due_date%TYPE
  );

  PROCEDURE return_book (
    p_loan_id IN  loans.loan_id%TYPE,
    p_fine    OUT loans.fine%TYPE
  );

  FUNCTION active_loan_count (
    p_member_id IN loans.member_id%TYPE
  ) RETURN PLS_INTEGER;

END library_pkg;
/

CREATE OR REPLACE PACKAGE BODY library_pkg AS

  FUNCTION active_loan_count (
    p_member_id IN loans.member_id%TYPE
  ) RETURN PLS_INTEGER
  IS
    v_count PLS_INTEGER;
  BEGIN
    SELECT COUNT(*) INTO v_count
    FROM   loans
    WHERE  member_id = p_member_id
    AND    return_date IS NULL;
    RETURN v_count;
  END active_loan_count;

  PROCEDURE borrow_book (
    p_book_id   IN  loans.book_id%TYPE,
    p_member_id IN  loans.member_id%TYPE,
    p_loan_id   OUT loans.loan_id%TYPE,
    p_due_date  OUT loans.due_date%TYPE
  ) IS
    v_available books.available_copies%TYPE;
    v_active    members.active%TYPE;
  BEGIN
    -- Validate member
    BEGIN
      SELECT active INTO v_active
      FROM   members
      WHERE  member_id = p_member_id;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20003, 'Member ' || p_member_id || ' not found.');
    END;

    IF v_active <> 'Y' THEN
      RAISE_APPLICATION_ERROR(-20003, 'Member ' || p_member_id || ' is inactive.');
    END IF;

    IF active_loan_count(p_member_id) >= c_max_loans THEN
      RAISE_APPLICATION_ERROR(-20004,
        'Member ' || p_member_id || ' already has ' || c_max_loans || ' active loans.');
    END IF;

    -- Lock the book row so concurrent borrows cannot oversell copies
    BEGIN
      SELECT available_copies INTO v_available
      FROM   books
      WHERE  book_id = p_book_id
      FOR UPDATE;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20001, 'Book ' || p_book_id || ' not found.');
    END;

    IF v_available <= 0 THEN
      RAISE_APPLICATION_ERROR(-20002, 'No copies of book ' || p_book_id || ' available.');
    END IF;

    UPDATE books
    SET    available_copies = available_copies - 1
    WHERE  book_id = p_book_id;

    p_due_date := TRUNC(SYSDATE) + c_loan_days;

    INSERT INTO loans (book_id, member_id, loan_date, due_date)
    VALUES (p_book_id, p_member_id, SYSDATE, p_due_date)
    RETURNING loan_id INTO p_loan_id;

    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK;
      RAISE;
  END borrow_book;

  PROCEDURE return_book (
    p_loan_id IN  loans.loan_id%TYPE,
    p_fine    OUT loans.fine%TYPE
  ) IS
    v_book_id   loans.book_id%TYPE;
    v_due_date  loans.due_date%TYPE;
    v_days_late PLS_INTEGER;
  BEGIN
    BEGIN
      SELECT book_id, due_date
      INTO   v_book_id, v_due_date
      FROM   loans
      WHERE  loan_id = p_loan_id
      AND    return_date IS NULL
      FOR UPDATE;
    EXCEPTION
      WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20005,
          'Loan ' || p_loan_id || ' not found or already returned.');
    END;

    v_days_late := GREATEST(TRUNC(SYSDATE) - TRUNC(v_due_date), 0);
    p_fine      := v_days_late * c_fine_per_day;

    UPDATE loans
    SET    return_date = SYSDATE,
           fine        = p_fine
    WHERE  loan_id = p_loan_id;

    UPDATE books
    SET    available_copies = available_copies + 1
    WHERE  book_id = v_book_id;

    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN
      ROLLBACK;
      RAISE;
  END return_book;

END library_pkg;
/

PROMPT Package LIBRARY_PKG compiled.
