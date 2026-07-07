-- Sample data for demo / manual testing
INSERT INTO books (title, author, isbn, published_year, total_copies, available_copies)
VALUES ('Clean Code', 'Robert C. Martin', '9780132350884', 2008, 3, 3);
INSERT INTO books (title, author, isbn, published_year, total_copies, available_copies)
VALUES ('Effective Java', 'Joshua Bloch', '9780134685991', 2018, 2, 2);
INSERT INTO books (title, author, isbn, published_year, total_copies, available_copies)
VALUES ('Oracle PL/SQL Programming', 'Steven Feuerstein', '9781449324452', 2014, 2, 2);
INSERT INTO books (title, author, isbn, published_year, total_copies, available_copies)
VALUES ('Designing Data-Intensive Applications', 'Martin Kleppmann', '9781449373320', 2017, 1, 1);

INSERT INTO members (full_name, email) VALUES ('Arben Hoxha', 'arben.hoxha@librarysystem.dev');
INSERT INTO members (full_name, email) VALUES ('Elira Kola',  'elira.kola@librarysystem.dev');
INSERT INTO members (full_name, email, active) VALUES ('Test Inactive', 'inactive@librarysystem.dev', 'N');

COMMIT;
PROMPT Seed data loaded.
