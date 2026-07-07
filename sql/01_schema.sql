-- Library Management System - Schema
-- Oracle 21c XE (works on 12c+; uses identity columns)
-- Run as the application user (e.g. LIBRARY_APP)

-- Drop in dependency order (safe to re-run)
BEGIN
  FOR t IN (SELECT table_name FROM user_tables
            WHERE table_name IN ('LOANS','MEMBERS','BOOKS')) LOOP
    EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS';
  END LOOP;
END;
/

CREATE TABLE books (
  book_id          NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  title            VARCHAR2(200) NOT NULL,
  author           VARCHAR2(120) NOT NULL,
  isbn             VARCHAR2(20)  NOT NULL,
  published_year   NUMBER(4),
  total_copies     NUMBER(4) DEFAULT 1 NOT NULL,
  available_copies NUMBER(4) DEFAULT 1 NOT NULL,
  CONSTRAINT uq_books_isbn      UNIQUE (isbn),
  CONSTRAINT ck_books_total     CHECK (total_copies >= 0),
  CONSTRAINT ck_books_available CHECK (available_copies BETWEEN 0 AND total_copies)
);

CREATE TABLE members (
  member_id   NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  full_name   VARCHAR2(120) NOT NULL,
  email       VARCHAR2(150) NOT NULL,
  joined_date DATE DEFAULT SYSDATE NOT NULL,
  active      CHAR(1) DEFAULT 'Y' NOT NULL,
  CONSTRAINT uq_members_email  UNIQUE (email),
  CONSTRAINT ck_members_active CHECK (active IN ('Y','N'))
);

CREATE TABLE loans (
  loan_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  book_id     NUMBER NOT NULL,
  member_id   NUMBER NOT NULL,
  loan_date   DATE DEFAULT SYSDATE NOT NULL,
  due_date    DATE NOT NULL,
  return_date DATE,
  fine        NUMBER(8,2) DEFAULT 0 NOT NULL,
  CONSTRAINT fk_loans_book   FOREIGN KEY (book_id)   REFERENCES books (book_id),
  CONSTRAINT fk_loans_member FOREIGN KEY (member_id) REFERENCES members (member_id),
  CONSTRAINT ck_loans_dates  CHECK (return_date IS NULL OR return_date >= loan_date)
);

-- Frequent access paths
CREATE INDEX ix_loans_member_open ON loans (member_id, return_date);
CREATE INDEX ix_loans_book        ON loans (book_id);

PROMPT Schema created.
