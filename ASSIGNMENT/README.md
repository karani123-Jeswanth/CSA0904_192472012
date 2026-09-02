# Smart Library Management System

A zero-dependency Java 11+ implementation of member-aware borrowing, FIFO reservations, fines, and overdue notifications. Existing Python files are intentionally preserved.

## Features

- Encapsulated, validated `Member` and `Book` hierarchies with runtime policy polymorphism.
- Student/faculty borrow limits and daily fine rates; printed, e-book, and reference-book policies.
- Thread-safe inventory and registries using `ConcurrentHashMap`, `Hashtable`, `HashSet`, and `TreeSet`.
- FIFO `ArrayList` waitlists, duplicate protection, safe `Iterator`/`ListIterator` traversal/removal, and `wait/notifyAll` wakeups.
- Runnable checkout/return transactions plus a daemon overdue scanner with explicit thread priorities.
- ANSI `Scanner` CLI and a built-in `com.sun.net.httpserver.HttpServer` dashboard.

## Compile and run (Windows PowerShell)

```powershell
Set-Location C:\Users\jeswa\smart_library_app
New-Item -ItemType Directory -Force out | Out-Null
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src\main\java | ForEach-Object FullName)
java -cp out main.Main
```

The application starts with six realistic demo members and eight available books. Run the HTTP dashboard without the interactive CLI:

```powershell
java -cp out main.Main --server 8080 --no-cli
# http://localhost:8080/
# GET /api/status, /api/books, /api/members, /api/loans, /api/reservations
```

The dashboard includes forms for registering members, cataloging books, issuing/returning
books, and placing reservations. The same operations are available with URL-encoded or
flat JSON POST bodies:

```text
POST /api/members       id, name, email, type (STUDENT|FACULTY)
POST /api/books         isbn, title, author, type (PRINTED|EBOOK|REFERENCE)
POST /api/loans         action (issue|return), memberId, isbn
POST /api/reservations  memberId, isbn
```

Successful creates return `201`; successful returns return `200`. Validation and domain
errors return a JSON `400` response with an `error` message.

Use `Ctrl+C` to stop server-only mode. No external dependencies are required.
