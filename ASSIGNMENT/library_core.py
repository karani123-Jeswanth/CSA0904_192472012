import threading
from concurrent.futures import ThreadPoolExecutor
from datetime import date, datetime, timedelta
from typing import Dict, List, Optional


class BookUnavailableException(Exception):
    """Raised when a requested book is unavailable for the requested action."""


class BorrowLimitExceededException(Exception):
    """Raised when a member attempts to borrow beyond their allowed limit."""


class DuplicateReservationException(Exception):
    """Raised when the same member attempts to reserve the same book again."""


class MemberNotFoundException(Exception):
    """Raised when a member ID cannot be located."""


class ReferenceOnlyItemException(Exception):
    """Raised when a reference item is attempted to be checked out."""


class Member:
    """Base member model with encapsulated properties and polymorphic behavior."""

    def __init__(self, member_id: str, name: str, email: str, membership_type: str):
        self._id = member_id
        self._name = name
        self._email = email
        self._membership_type = membership_type
        self._borrowed_books: List[str] = []
        self._borrow_limit = 0
        self._fine_rate = 0.0

    @property
    def id(self):
        return self._id

    @id.setter
    def id(self, value: str):
        self._id = value

    @property
    def name(self):
        return self._name

    @name.setter
    def name(self, value: str):
        self._name = value

    @property
    def email(self):
        return self._email

    @email.setter
    def email(self, value: str):
        self._email = value

    @property
    def membership_type(self):
        return self._membership_type

    @membership_type.setter
    def membership_type(self, value: str):
        self._membership_type = value

    @property
    def borrowed_books(self):
        return list(self._borrowed_books)

    @borrowed_books.setter
    def borrowed_books(self, value):
        self._borrowed_books = list(value)

    @property
    def borrow_limit(self):
        return self._borrow_limit

    @borrow_limit.setter
    def borrow_limit(self, value: int):
        self._borrow_limit = int(value)

    @property
    def fine_rate(self):
        return self._fine_rate

    @fine_rate.setter
    def fine_rate(self, value: float):
        self._fine_rate = float(value)

    def add_borrowed_book(self, isbn: str):
        if isbn not in self._borrowed_books:
            self._borrowed_books.append(isbn)

    def remove_borrowed_book(self, isbn: str):
        if isbn in self._borrowed_books:
            self._borrowed_books.remove(isbn)

    def calculate_fine(self, days_overdue: int) -> float:
        return max(0.0, float(days_overdue) * self.fine_rate)

    def can_borrow(self) -> bool:
        return len(self._borrowed_books) < self.borrow_limit

    def to_dict(self):
        return {
            "member_id": self.id,
            "name": self.name,
            "email": self.email,
            "membership_type": self.membership_type,
            "borrow_limit": self.borrow_limit,
            "fine_rate": self.fine_rate,
            "borrowed_books": ", ".join(self.borrowed_books),
        }


class StudentMember(Member):
    def __init__(self, member_id: str, name: str, email: str):
        super().__init__(member_id, name, email, "STUDENT")
        self.borrow_limit = 3
        self.fine_rate = 1.0


class FacultyMember(Member):
    def __init__(self, member_id: str, name: str, email: str):
        super().__init__(member_id, name, email, "FACULTY")
        self.borrow_limit = 10
        self.fine_rate = 0.2


class PremiumMember(Member):
    def __init__(self, member_id: str, name: str, email: str):
        super().__init__(member_id, name, email, "PREMIUM")
        self.borrow_limit = 5
        self.fine_rate = 0.5


class Book:
    """Base book model with encapsulated accessors and polymorphic loan rules."""

    def __init__(self, isbn: str, title: str, author: str, category: str, total_copies: int):
        self._isbn = isbn
        self._title = title
        self._author = author
        self._category = category
        self._total_copies = int(total_copies)
        self._available_copies = int(total_copies)

    @property
    def isbn(self):
        return self._isbn

    @isbn.setter
    def isbn(self, value: str):
        self._isbn = value

    @property
    def title(self):
        return self._title

    @title.setter
    def title(self, value: str):
        self._title = value

    @property
    def author(self):
        return self._author

    @author.setter
    def author(self, value: str):
        self._author = value

    @property
    def category(self):
        return self._category

    @category.setter
    def category(self, value: str):
        self._category = value

    @property
    def total_copies(self):
        return self._total_copies

    @total_copies.setter
    def total_copies(self, value: int):
        self._total_copies = max(0, int(value))

    @property
    def available_copies(self):
        return self._available_copies

    @available_copies.setter
    def available_copies(self, value: int):
        self._available_copies = max(0, min(int(value), self._total_copies))

    def is_circulable(self) -> bool:
        return True

    def get_loan_period(self) -> int:
        return 14

    def to_dict(self):
        return {
            "isbn": self.isbn,
            "title": self.title,
            "author": self.author,
            "category": self.category,
            "total_copies": self.total_copies,
            "available_copies": self.available_copies,
            "loan_period_days": self.get_loan_period(),
            "circulable": self.is_circulable(),
        }


class RegularBook(Book):
    def __init__(self, isbn: str, title: str, author: str, category: str, total_copies: int):
        super().__init__(isbn, title, author, category, total_copies)

    def get_loan_period(self) -> int:
        return 14


class ReferenceBook(Book):
    def __init__(self, isbn: str, title: str, author: str, category: str, total_copies: int = 1):
        super().__init__(isbn, title, author, category, total_copies)
        self.available_copies = 0

    def is_circulable(self) -> bool:
        return False

    def get_loan_period(self) -> int:
        return 0


class DigitalResource(Book):
    def __init__(self, isbn: str, title: str, author: str, category: str, total_copies: int):
        super().__init__(isbn, title, author, category, total_copies)

    def get_loan_period(self) -> int:
        return 7


class Reservation:
    def __init__(self, reservation_id: str, member_id: str, isbn: str, timestamp: Optional[datetime] = None):
        self._reservation_id = reservation_id
        self._member_id = member_id
        self._isbn = isbn
        self._timestamp = timestamp or datetime.now()
        self._status = "PENDING"

    @property
    def reservation_id(self):
        return self._reservation_id

    @reservation_id.setter
    def reservation_id(self, value: str):
        self._reservation_id = value

    @property
    def member_id(self):
        return self._member_id

    @member_id.setter
    def member_id(self, value: str):
        self._member_id = value

    @property
    def isbn(self):
        return self._isbn

    @isbn.setter
    def isbn(self, value: str):
        self._isbn = value

    @property
    def timestamp(self):
        return self._timestamp

    @timestamp.setter
    def timestamp(self, value: datetime):
        self._timestamp = value

    @property
    def status(self):
        return self._status

    @status.setter
    def status(self, value: str):
        self._status = value.upper()

    def to_dict(self):
        return {
            "reservation_id": self.reservation_id,
            "member_id": self.member_id,
            "isbn": self.isbn,
            "timestamp": self.timestamp.isoformat(timespec="seconds"),
            "status": self.status,
        }


class Notification:
    def __init__(self, notification_id: str, member_id: str, message: str, notification_type: str, is_read: bool = False):
        self._notification_id = notification_id
        self._member_id = member_id
        self._message = message
        self._timestamp = datetime.now()
        self._notification_type = notification_type
        self._is_read = is_read

    @property
    def notification_id(self):
        return self._notification_id

    @notification_id.setter
    def notification_id(self, value: str):
        self._notification_id = value

    @property
    def member_id(self):
        return self._member_id

    @member_id.setter
    def member_id(self, value: str):
        self._member_id = value

    @property
    def message(self):
        return self._message

    @message.setter
    def message(self, value: str):
        self._message = value

    @property
    def timestamp(self):
        return self._timestamp

    @timestamp.setter
    def timestamp(self, value: datetime):
        self._timestamp = value

    @property
    def notification_type(self):
        return self._notification_type

    @notification_type.setter
    def notification_type(self, value: str):
        self._notification_type = value.upper()

    @property
    def is_read(self):
        return self._is_read

    @is_read.setter
    def is_read(self, value: bool):
        self._is_read = bool(value)

    def to_dict(self):
        return {
            "notification_id": self.notification_id,
            "member_id": self.member_id,
            "message": self.message,
            "timestamp": self.timestamp.isoformat(timespec="seconds"),
            "notification_type": self.notification_type,
            "is_read": self.is_read,
        }


class Inventory:
    """A thread-safe inventory manager holding books by ISBN."""

    def __init__(self):
        self._books: Dict[str, Book] = {}
        self._checkout_state: Dict[str, Dict[str, int]] = {}
        self._lock = threading.RLock()

    @property
    def books(self):
        return dict(self._books)

    @property
    def lock(self):
        return self._lock

    def add_book(self, book: Book):
        with self._lock:
            self._books[book.isbn] = book
            self._checkout_state[book.isbn] = {
                "total_copies": book.total_copies,
                "available_copies": book.available_copies,
            }

    def get_book(self, isbn: str) -> Book:
        with self._lock:
            book = self._books.get(isbn)
            if book is None:
                raise BookUnavailableException(f"Book with ISBN {isbn} was not found.")
            return book

    def update_book(self, isbn: str, **kwargs):
        with self._lock:
            book = self.get_book(isbn)
            for key, value in kwargs.items():
                if hasattr(book, key):
                    setattr(book, key, value)
            self._checkout_state[isbn] = {
                "total_copies": book.total_copies,
                "available_copies": book.available_copies,
            }

    def remove_book(self, isbn: str):
        with self._lock:
            if isbn in self._books:
                self._books.pop(isbn)
                self._checkout_state.pop(isbn, None)

    def all_books(self) -> List[Book]:
        with self._lock:
            return list(self._books.values())

    def search_books(self, query: str) -> List[Book]:
        q = query.strip().lower()
        if not q:
            return self.all_books()
        matches = []
        for book in self._books.values():
            searchable = " ".join(
                [book.title, book.author, book.isbn, book.category]
            ).lower()
            if q in searchable:
                matches.append(book)
        return matches

    def issue_copy(self, isbn: str):
        with self._lock:
            book = self.get_book(isbn)
            if not book.is_circulable():
                raise ReferenceOnlyItemException(f"{book.title} is reference-only and cannot be borrowed.")
            if book.available_copies <= 0:
                raise BookUnavailableException(f"No copies of {book.title} are available right now.")
            book.available_copies = book.available_copies - 1
            self._checkout_state[isbn] = {
                "total_copies": book.total_copies,
                "available_copies": book.available_copies,
            }
            return book

    def return_copy(self, isbn: str):
        with self._lock:
            book = self.get_book(isbn)
            if book.available_copies < book.total_copies:
                book.available_copies = book.available_copies + 1
            self._checkout_state[isbn] = {
                "total_copies": book.total_copies,
                "available_copies": book.available_copies,
            }
            return book


class LibrarySystem:
    """Central application logic coordinating members, inventory, reservations, notifications, and analytics."""

    def __init__(self):
        self.members: Dict[str, Member] = {}
        self.inventory = Inventory()
        self.reservations: List[Reservation] = []
        self.notifications: List[Notification] = []
        self.loans: List[Dict] = []
        self.fine_ledger: List[Dict] = []
        self.lock = threading.RLock()
        self._reservation_counter = 1
        self._loan_counter = 1
        self._notification_counter = 1

    def _next_reservation_id(self):
        value = f"RES-{self._reservation_counter:04d}"
        self._reservation_counter += 1
        return value

    def _next_loan_id(self):
        value = f"LN-{self._loan_counter:04d}"
        self._loan_counter += 1
        return value

    def _next_notification_id(self):
        value = f"NT-{self._notification_counter:04d}"
        self._notification_counter += 1
        return value

    def add_member(self, member: Member):
        with self.lock:
            self.members[member.id] = member

    def get_member(self, member_id: str) -> Member:
        with self.lock:
            member = self.members.get(member_id)
            if member is None:
                raise MemberNotFoundException(f"Member with ID {member_id} was not found.")
            return member

    def add_book(self, book: Book):
        with self.lock:
            self.inventory.add_book(book)

    def get_book(self, isbn: str) -> Book:
        return self.inventory.get_book(isbn)

    def issue_book(self, member_id: str, isbn: str):
        with self.lock:
            member = self.get_member(member_id)
            book = self.get_book(isbn)

            if not book.is_circulable():
                raise ReferenceOnlyItemException(f"{book.title} is available only for in-library consultation.")
            if not member.can_borrow():
                raise BorrowLimitExceededException(
                    f"{member.name} has reached their borrowing limit ({member.borrow_limit})."
                )
            if book.available_copies <= 0:
                raise BookUnavailableException(f"{book.title} is currently out of stock.")

            self.inventory.issue_copy(isbn)
            member.add_borrowed_book(isbn)
            due_date = date.today() + timedelta(days=book.get_loan_period())
            loan_record = {
                "loan_id": self._next_loan_id(),
                "member_id": member_id,
                "isbn": isbn,
                "book_title": book.title,
                "checkout_date": date.today(),
                "due_date": due_date,
                "status": "ACTIVE",
            }
            self.loans.append(loan_record)
            return loan_record

    def return_book(self, member_id: str, isbn: str, returned_date: Optional[date] = None):
        with self.lock:
            member = self.get_member(member_id)
            book = self.get_book(isbn)
            active_loan = None
            for loan in self.loans:
                if loan["member_id"] == member_id and loan["isbn"] == isbn and loan["status"] == "ACTIVE":
                    active_loan = loan
                    break
            if active_loan is None:
                raise BookUnavailableException(f"No active loan exists for member {member_id} and ISBN {isbn}.")

            return_day = returned_date or date.today()
            due_date = active_loan["due_date"]
            overdue_days = max(0, (return_day - due_date).days)
            fine = member.calculate_fine(overdue_days)

            self.inventory.return_copy(isbn)
            member.remove_borrowed_book(isbn)
            active_loan["status"] = "RETURNED"
            active_loan["returned_date"] = return_day
            active_loan["days_overdue"] = overdue_days
            active_loan["fine_amount"] = fine

            if fine > 0:
                self.fine_ledger.append(
                    {
                        "member_id": member_id,
                        "member_name": member.name,
                        "isbn": isbn,
                        "book_title": book.title,
                        "fine_amount": round(fine, 2),
                        "days_overdue": overdue_days,
                    }
                )
                self._add_notification(
                    member_id,
                    f"Fine of ${fine:.2f} assessed for overdue return of {book.title}.",
                    "OVERDUE",
                )

            self._check_and_fulfill_reservations(isbn)
            return active_loan

    def reserve_book(self, member_id: str, isbn: str):
        with self.lock:
            self.get_member(member_id)
            self.get_book(isbn)
            for reservation in self.reservations:
                if reservation.member_id == member_id and reservation.isbn == isbn and reservation.status == "PENDING":
                    raise DuplicateReservationException(
                        f"Member {member_id} already has a pending reservation for {isbn}."
                    )
            reservation = Reservation(self._next_reservation_id(), member_id, isbn)
            self.reservations.append(reservation)
            self._add_notification(
                member_id,
                f"Reservation placed for {isbn}. You are queued in the waitlist.",
                "RESERVATION_READY",
            )
            return reservation

    def _check_and_fulfill_reservations(self, isbn: str):
        with self.lock:
            book = self.get_book(isbn)
            if book.available_copies <= 0:
                return
            pending = [r for r in self.reservations if r.isbn == isbn and r.status == "PENDING"]
            if not pending:
                return
            next_reservation = pending[0]
            next_reservation.status = "FULFILLED"
            self._add_notification(
                next_reservation.member_id,
                f"Your reservation for {book.title} is ready for collection.",
                "RESERVATION_READY",
            )

    def _add_notification(self, member_id: str, message: str, notification_type: str):
        with self.lock:
            notification = Notification(
                self._next_notification_id(),
                member_id,
                message,
                notification_type,
                False,
            )
            self.notifications.append(notification)
            return notification

    def scan_overdue_notifications(self):
        with self.lock:
            today = date.today()
            for loan in self.loans:
                if loan["status"] != "ACTIVE":
                    continue
                if loan["due_date"] < today:
                    member = self.get_member(loan["member_id"])
                    overdue_days = (today - loan["due_date"]).days
                    message = (
                        f"Book {loan['book_title']} is overdue by {overdue_days} day(s). "
                        f"Fine accrued: ${member.calculate_fine(overdue_days):.2f}."
                    )
                    exists = any(
                        notification.member_id == loan["member_id"]
                        and notification.notification_type == "OVERDUE"
                        and notification.message.startswith(f"Book {loan['book_title']} is overdue")
                        for notification in self.notifications
                    )
                    if not exists:
                        self._add_notification(loan["member_id"], message, "OVERDUE")

    def simulate_concurrent_borrowing(self, isbn: str = "978-0-13-235088-4", member_ids: Optional[List[str]] = None):
        if member_ids is None:
            member_ids = ["M-1001", "M-1002", "M-1003", "M-1004"]
        results = []
        with ThreadPoolExecutor(max_workers=len(member_ids)) as executor:
            futures = [executor.submit(self.issue_book, member_id, isbn) for member_id in member_ids]
            for future in futures:
                try:
                    results.append(future.result())
                except Exception as exc:  # pragma: no cover - UI catches original error
                    results.append({"error": str(exc)})
        return results

    def seed_demo_data(self):
        with self.lock:
            if self.members or self.inventory.books or self.loans:
                return

            members = [
                StudentMember("M-1001", "Ava Patel", "ava.patel@college.edu"),
                StudentMember("M-1002", "Leo Grant", "leo.grant@college.edu"),
                FacultyMember("M-1003", "Dr. Nia Chen", "nia.chen@university.edu"),
                PremiumMember("M-1004", "Samir Ali", "samir.ali@premium.net"),
            ]
            for member in members:
                self.add_member(member)

            books = [
                RegularBook("978-0-13-235088-4", "Clean Code", "Robert C. Martin", "Software", 3),
                RegularBook("978-1-78439-180-2", "Data Science for Business", "Foster Provost", "Analytics", 2),
                RegularBook("978-0-07-180854-9", "Design Patterns", "Erich Gamma", "Software", 2),
                ReferenceBook("978-0-262-13472-9", "The Art of Computer Programming", "Donald Knuth", "Reference", 1),
                DigitalResource("978-1-119-06136-6", "Machine Learning Yearning", "Andrew Ng", "Digital", 4),
            ]
            for book in books:
                self.add_book(book)

            self.members["M-1001"].add_borrowed_book("978-0-13-235088-4")
            self.inventory.issue_copy("978-0-13-235088-4")
            self.loans.append(
                {
                    "loan_id": self._next_loan_id(),
                    "member_id": "M-1001",
                    "isbn": "978-0-13-235088-4",
                    "book_title": "Clean Code",
                    "checkout_date": date.today() - timedelta(days=18),
                    "due_date": date.today() - timedelta(days=4),
                    "status": "ACTIVE",
                }
            )

            self.members["M-1002"].add_borrowed_book("978-1-78439-180-2")
            self.inventory.issue_copy("978-1-78439-180-2")
            self.loans.append(
                {
                    "loan_id": self._next_loan_id(),
                    "member_id": "M-1002",
                    "isbn": "978-1-78439-180-2",
                    "book_title": "Data Science for Business",
                    "checkout_date": date.today() - timedelta(days=12),
                    "due_date": date.today() - timedelta(days=1),
                    "status": "ACTIVE",
                }
            )

            self.members["M-1003"].add_borrowed_book("978-0-07-180854-9")
            self.inventory.issue_copy("978-0-07-180854-9")
            self.loans.append(
                {
                    "loan_id": self._next_loan_id(),
                    "member_id": "M-1003",
                    "isbn": "978-0-07-180854-9",
                    "book_title": "Design Patterns",
                    "checkout_date": date.today() - timedelta(days=8),
                    "due_date": date.today() - timedelta(days=2),
                    "status": "ACTIVE",
                }
            )

            self._add_notification(
                "M-1001",
                "Book Clean Code is overdue by 4 days. Please return it promptly.",
                "OVERDUE",
            )
            self._add_notification(
                "M-1002",
                "Book Data Science for Business is due soon.",
                "DUE_SOON",
            )

            reservation = Reservation(self._next_reservation_id(), "M-1004", "978-0-13-235088-4")
            self.reservations.append(reservation)

    def get_overdue_loans(self):
        today = date.today()
        overdue = []
        for loan in self.loans:
            if loan["status"] == "ACTIVE" and loan["due_date"] < today:
                member = self.get_member(loan["member_id"])
                overdue.append(
                    {
                        "loan_id": loan["loan_id"],
                        "member_id": loan["member_id"],
                        "member_name": member.name,
                        "book_title": loan["book_title"],
                        "isbn": loan["isbn"],
                        "due_date": loan["due_date"].isoformat(),
                        "days_overdue": (today - loan["due_date"]).days,
                    }
                )
        return overdue

    def get_dashboard_summary(self):
        total_members = len(self.members)
        total_books = len(self.inventory.books)
        active_loans = sum(1 for loan in self.loans if loan["status"] == "ACTIVE")
        overdue_count = len(self.get_overdue_loans())
        total_fine_revenue = round(sum(item["fine_amount"] for item in self.fine_ledger), 2)
        utilization = 0.0
        if total_books:
            utilization = round((sum(book.available_copies for book in self.inventory.all_books()) / sum(book.total_copies for book in self.inventory.all_books())) * 100, 2)
        return {
            "total_members": total_members,
            "total_books": total_books,
            "active_loans": active_loans,
            "overdue_count": overdue_count,
            "fine_revenue": total_fine_revenue,
            "inventory_utilization": utilization,
            "notifications": len(self.notifications),
        }
