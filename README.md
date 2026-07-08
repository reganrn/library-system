# Library Management System

A console application for managing a library's books, members, and loans - built on **Java (JDBC)** with **Oracle Database**, where all transactional business logic lives in a **PL/SQL package** rather than application code.

```
┌─────────────┐     ┌───────────────┐     ┌────────────┐     ┌───────────────────────┐
│  ConsoleUI  │ --> │ LibraryService│ --> │    DAOs    │ --> │  Oracle DB            │
│  (input/    │     │ (input        │     │ (JDBC +    │     │  LIBRARY_PKG (PL/SQL) │
│   output)   │     │  validation)  │     │  Callable- │     │  = business rules,    │
│             │     │               │     │  Statement)│     │    transactions       │
└─────────────┘     └───────────────┘     └────────────┘     └───────────────────────┘
```

## Why the logic is in PL/SQL

Borrowing a book is not one statement - it's *lock the member, check the loan limit, lock the book row, decrement copies, insert loan* as one atomic unit. Doing this in Java would mean multiple round trips and race conditions between check and update. `LIBRARY_PKG.BORROW_BOOK` does it in a single database call with `SELECT ... FOR UPDATE` row locking, so two clients can never borrow the last copy or push one member past the active-loan limit simultaneously. The Java layer maps the package's `RAISE_APPLICATION_ERROR` codes (ORA-20001…20005) into clean user-facing messages.

## Features

- Book catalogue: list, search by title, add titles with multiple copies
- Member registry: list, register (unique email enforced by constraint)
- Borrow with business rules enforced in-database:
  - member must exist and be active
  - max **3 active loans** per member
  - copies decremented atomically under row lock
  - due date = loan date + **14 days**
- Return with automatic **fine calculation** (0.50/day late)
- Open-loan and full-history views (joined queries)

## Tech stack

| Layer          | Technology                                                             |
|----------------|------------------------------------------------------------------------|
| Language       | Java 17+ (tested on 21)                                                |
| Database       | Oracle 21c XE (any 12c+ works)                                         |
| DB access      | JDBC - `PreparedStatement` for queries, `CallableStatement` for PL/SQL |
| Business logic | PL/SQL package (`sql/02_library_pkg.sql`)                              |
| Build          | Maven (shade plugin -> runnable jar)                                    |

## Getting started

### 1. Start Oracle (Docker, easiest)

```bash
docker run -d --name oracle-xe -p 1521:1521 \
  -e ORACLE_PASSWORD=oracle gvenzl/oracle-xe:21-slim
```

Create the application user:

```sql
-- connect as SYSTEM to XEPDB1
CREATE USER library_app IDENTIFIED BY library_app_dev;
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO library_app;
```

### 2. Create the schema

Run the scripts **in order** as `library_app` (SQL*Plus, SQLcl, or SQL Developer):

```
sql/01_schema.sql       -- tables, constraints, indexes
sql/02_library_pkg.sql  -- the PL/SQL business-logic package
sql/03_seed_data.sql    -- sample books and members
```

### 3. Configure the connection

```bash
cp src/main/resources/db.properties.example db.properties
# Review db.properties if the Oracle host, service, or credentials differ
```

`db.properties` is git-ignored - credentials never enter the repository.

### 4. Run

```bash
mvn package
java -jar target/library-system-1.0.0.jar
```

or during development: `mvn compile exec:java`

## Example session

```
=== Library Management System ===
1) List books        4) List members     7) Return a book
2) Search books      5) Register member  8) Open loans
3) Add book          6) Borrow a book    9) Loan history
0) Exit
> 6
Book id: 1
Member id: 1
Loan #1 created, due 2026-07-19
> 6
Book id: 1
Member id: 3
!! Member 3 is inactive.
```

## Project structure

```
sql/                          database scripts (run once, in order)
src/main/java/com/regan/library/
  config/Db.java              connection factory, reads db.properties
  model/                      Book, Member, Loan (immutable POJOs)
  dao/                        JDBC data access; LoanDao calls LIBRARY_PKG
  service/LibraryService.java single entry point for the UI
  ui/ConsoleUI.java           menu, input parsing, output formatting
  exception/                  LibraryException with ORA error-code mapping
```

## What was hard

- **Getting the transaction boundary right.** My first version decremented `available_copies` from Java in a separate statement from the loan insert - under concurrency that oversells copies. Moving the whole operation into one PL/SQL procedure with `FOR UPDATE` locking on both the member and book rows fixed it and taught me why check-then-act logic belongs next to the data.
- **Translating Oracle errors into UX.** `RAISE_APPLICATION_ERROR` messages arrive prefixed with `ORA-20004:` and stack noise; the DAO layer strips and maps them by error code so the console shows *"Member 2 already has 3 active loans."* instead of a stack trace.

## Roadmap

- [ ] Rebuild as a Spring Boot REST API (in progress -> [library-api](https://github.com/reganrn/library-api))
- [ ] Reservation queue for unavailable books
- [ ] Reports via PL/SQL cursors (most-borrowed titles, member activity)

## License

MIT
