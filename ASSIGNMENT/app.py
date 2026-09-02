import pandas as pd
import streamlit as st
from datetime import date

from library_core import (
    BorrowLimitExceededException,
    BookUnavailableException,
    DuplicateReservationException,
    DigitalResource,
    FacultyMember,
    LibrarySystem,
    MemberNotFoundException,
    PremiumMember,
    ReferenceBook,
    RegularBook,
    ReferenceOnlyItemException,
    StudentMember,
)


st.set_page_config(page_title="Smart Library Management", layout="wide")

st.markdown(
    """
    <style>
    :root {
        --bg: #0b1220;
        --panel: #111b2d;
        --panel-alt: #172338;
        --panel-soft: #1b2a42;
        --text: #e5edf9;
        --muted: #a9b8d1;
        --accent: #7cc7ff;
        --accent-2: #6ee7b7;
        --warning: #fbbf24;
        --danger: #f87171;
        --shadow: rgba(15, 23, 42, 0.45);
    }
    .stApp {
        background: linear-gradient(180deg, #08111d 0%, #111b2d 100%);
        color: var(--text);
        animation: pageFade 0.75s ease-out;
    }
    @keyframes pageFade {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
    }
    @keyframes floatGlow {
        0% { transform: translateY(0); box-shadow: 0 0 0 rgba(124, 199, 255, 0.0); }
        50% { transform: translateY(-2px); box-shadow: 0 12px 26px rgba(124, 199, 255, 0.18); }
        100% { transform: translateY(0); box-shadow: 0 0 0 rgba(124, 199, 255, 0.0); }
    }
    .block-container {
        padding-top: 1.5rem;
        padding-bottom: 2rem;
    }
    [data-testid="stHeader"] {
        background: rgba(8, 17, 29, 0.7);
        box-shadow: 0 1px 0 rgba(255,255,255,0.08);
    }
    .stTabs [role="tablist"] {
        background: rgba(23, 35, 56, 0.9);
        border-radius: 0.75rem;
        padding: 0.25rem;
        gap: 0.25rem;
    }
    .stTabs [role="tab"] {
        color: var(--muted);
        border-radius: 0.6rem;
        font-weight: 600;
    }
    .stTabs [role="tab"][aria-selected="true"] {
        background: linear-gradient(135deg, #22365d, #16263f);
        color: var(--text);
        border: 1px solid rgba(124, 199, 255, 0.35);
    }
    .stDataFrame, .stTable {
        background: rgba(17, 27, 45, 0.8);
        border: 1px solid rgba(255,255,255,0.06);
        border-radius: 0.8rem;
        color: var(--text);
        animation: floatGlow 5s ease-in-out infinite;
    }
    div[data-testid="stMetricValue"] {
        color: var(--text);
    }
    div[data-testid="stMetricDelta"] {
        color: var(--accent-2);
    }
    .element-container > div > div {
        border-color: rgba(255,255,255,0.08) !important;
    }
    .stForm, .stSelectbox, .stTextInput, .stNumberInput {
        background: rgba(23, 35, 56, 0.7);
        border-radius: 0.7rem;
    }
    .stButton > button {
        background: linear-gradient(135deg, #3b82f6, #2563eb);
        color: white;
        border: none;
        border-radius: 0.7rem;
        font-weight: 600;
        transition: transform 0.2s ease, box-shadow 0.2s ease;
    }
    .stButton > button:hover {
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        transform: translateY(-1px);
        box-shadow: 0 10px 22px rgba(59,130,246,0.35);
    }
    .stSidebar {
        background: rgba(11, 18, 32, 0.96);
        border-right: 1px solid rgba(255,255,255,0.08);
    }
    .stSidebar .css-1d391kg {
        background: rgba(11, 18, 32, 0.96);
    }
    .login-card {
        background: linear-gradient(135deg, rgba(22, 38, 63, 0.85), rgba(15, 23, 42, 0.85));
        border: 1px solid rgba(124, 199, 255, 0.22);
        border-radius: 1rem;
        padding: 1.2rem;
        box-shadow: 0 18px 40px rgba(2, 6, 23, 0.45);
        animation: pageFade 0.8s ease-out;
    }
    .demo-user {
        background: rgba(17, 27, 45, 0.75);
        border: 1px solid rgba(255,255,255,0.06);
        border-radius: 0.7rem;
        padding: 0.7rem 0.8rem;
        margin-bottom: 0.5rem;
    }
    </style>
    """,
    unsafe_allow_html=True,
)

DEMO_USERS = {
    "admin": {"password": "admin123", "role": "Administrator", "member_id": None},
    "librarian": {"password": "librarian123", "role": "Librarian", "member_id": None},
    "ava": {"password": "ava123", "role": "Student Member", "member_id": "M-1001"},
    "leo": {"password": "leo123", "role": "Student Member", "member_id": "M-1002"},
    "nia": {"password": "nia123", "role": "Faculty Member", "member_id": "M-1003"},
    "samir": {"password": "samir123", "role": "Premium Member", "member_id": "M-1004"},
}


def get_user_profile(system: LibrarySystem):
    username = st.session_state.get("auth_user")
    if not username:
        return {"username": None, "role": "Guest", "member_id": None, "member": None, "is_staff": False, "is_member": False}
    profile = DEMO_USERS.get(username, {})
    member_id = profile.get("member_id")
    member = system.get_member(member_id) if member_id and member_id in system.members else None
    return {
        "username": username,
        "role": profile.get("role", "User"),
        "member_id": member_id,
        "member": member,
        "is_staff": profile.get("role") in {"Administrator", "Librarian"},
        "is_member": member is not None,
    }


def get_system() -> LibrarySystem:
    if "library_system" not in st.session_state:
        system = LibrarySystem()
        system.seed_demo_data()
        st.session_state.library_system = system
    return st.session_state.library_system


def render_metric_card(label: str, value: str, delta: str = ""):
    st.markdown(
        f"""
        <div class="metric-card" style="padding: 1rem; border-radius: 0.8rem; background: linear-gradient(135deg, #15233d, #1b2a42); border: 1px solid rgba(124, 199, 255, 0.18); margin-bottom: 0.5rem; box-shadow: 0 6px 18px rgba(15, 23, 42, 0.25); animation: floatGlow 5.5s ease-in-out infinite;">
            <div style="font-size: 0.78rem; color: #a9b8d1; letter-spacing: 0.04em; text-transform: uppercase;">{label}</div>
            <div style="font-size: 1.8rem; font-weight: 700; color: #f8fbff; margin-top: 0.25rem;">{value}</div>
            <div style="font-size: 0.76rem; color: #6ee7b7; margin-top: 0.15rem;">{delta}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )


def member_table_df(system: LibrarySystem):
    members = [member.to_dict() for member in system.members.values()]
    df = pd.DataFrame(members)
    if df.empty:
        return pd.DataFrame(columns=["member_id", "name", "email", "membership_type", "borrow_limit", "borrowed_books"])
    return df


def book_table_df(system: LibrarySystem):
    books = [book.to_dict() for book in system.inventory.all_books()]
    df = pd.DataFrame(books)
    if df.empty:
        return pd.DataFrame(columns=["isbn", "title", "author", "category", "available_copies", "total_copies", "circulable", "loan_period_days"])
    df["status"] = df.apply(
        lambda row: "Available" if row["available_copies"] > 0 else "Out of stock",
        axis=1,
    )
    df["status_badge"] = df.apply(
        lambda row: "✅ Available" if row["circulable"] and row["available_copies"] > 0 else "🚫 Unavailable" if not row["circulable"] else "📦 Out of stock",
        axis=1,
    )
    return df


def reservation_table_df(system: LibrarySystem):
    rows = [reservation.to_dict() for reservation in system.reservations]
    return pd.DataFrame(rows)


def notification_table_df(system: LibrarySystem):
    rows = [notification.to_dict() for notification in system.notifications]
    return pd.DataFrame(rows)


def loan_table_df(system: LibrarySystem):
    return pd.DataFrame(system.loans)


if "auth_user" not in st.session_state:
    st.session_state.auth_user = None

system = get_system()

with st.sidebar:
    if st.session_state.auth_user is None:
        st.markdown(
            """
            <div class="login-card">
                <h3 style="margin: 0 0 0.4rem 0; color: #f8fbff;">Library access</h3>
                <p style="margin: 0 0 1rem 0; color: #a9b8d1;">Sign in to use the system.</p>
            </div>
            """,
            unsafe_allow_html=True,
        )
        with st.form("login_form"):
            username = st.text_input("Username")
            password = st.text_input("Password", type="password")
            submitted = st.form_submit_button("Login")
        if submitted:
            user_record = DEMO_USERS.get(username)
            if user_record and user_record["password"] == password:
                st.session_state.auth_user = username
                st.success(f"Welcome back, {username}!")
                st.rerun()
            else:
                st.error("Invalid username or password.")

        st.markdown("<hr style='border-color: rgba(255,255,255,0.08);'>", unsafe_allow_html=True)
        st.subheader("Demo users")
        for user_name, user_data in DEMO_USERS.items():
            st.markdown(
                f"""
                <div class="demo-user">
                    <strong>{user_name}</strong><br>
                    <span style='color: #a9b8d1;'>{user_data['role']}</span><br>
                    <span style='font-size: 0.8rem; color: #7cc7ff;'>Password: {user_data['password']}</span>
                </div>
                """,
                unsafe_allow_html=True,
            )
        st.stop()

    st.header("Library Control")
    current_user = st.session_state.auth_user
    current_role = DEMO_USERS[current_user]["role"]
    st.markdown(
        f"""
        <div class="demo-user">
            <strong>{current_user}</strong><br>
            <span style='color: #a9b8d1;'>{current_role}</span>
        </div>
        """,
        unsafe_allow_html=True,
    )
    if st.button("Logout", use_container_width=True):
        st.session_state.auth_user = None
        st.rerun()

    if current_role in {"Administrator", "Librarian"}:
        if st.button("Reset demo data", use_container_width=True):
            st.session_state.library_system = LibrarySystem()
            st.session_state.library_system.seed_demo_data()
            st.success("Demo data restored.")
            st.rerun()

        if st.button("Scan overdue notifications", use_container_width=True):
            try:
                system.scan_overdue_notifications()
                st.toast("Automated overdue scan completed.")
            except Exception as exc:  # pragma: no cover
                st.error(f"Overdue scan failed: {exc}")
                st.toast(f"Scan failed: {exc}")

        if st.button("Run concurrency simulation", use_container_width=True):
            try:
                results = system.simulate_concurrent_borrowing("978-0-13-235088-4", ["M-1001", "M-1002", "M-1003", "M-1004"])
                successful = [result for result in results if isinstance(result, dict) and "error" not in result]
                failures = [result for result in results if isinstance(result, dict) and "error" in result]
                st.toast(f"Concurrency simulation finished: {len(successful)} successful, {len(failures)} failed.")
                if failures:
                    st.warning("Some simulated borrow attempts failed due to stock constraints, which is expected in a safe concurrent workflow.")
            except Exception as exc:  # pragma: no cover
                st.error(f"Concurrency simulation failed: {exc}")
                st.toast(f"Simulation failed: {exc}")
    else:
        st.info("Your workspace is personalized for your member account.")

    st.markdown("---")
    st.subheader("Quick filters")
    st.session_state.search_query = st.text_input("Global search", value="")

st.title("Smart Library Management System")
st.caption("Book Reservation and Overdue Notification System")

user_profile = get_user_profile(system)

if user_profile["is_staff"]:
    overview_tab, catalog_tab, circulation_tab, reservations_tab, reports_tab = st.tabs(
        ["Overview", "Catalog", "Circulation", "Reservations", "Reports"]
    )
else:
    overview_tab, my_loans_tab, my_reservations_tab, reports_tab = st.tabs(
        ["Overview", "My Loans", "My Reservations", "Reports"]
    )

with overview_tab:
    user_profile = get_user_profile(system)
    if user_profile["is_staff"]:
        st.subheader(f"Welcome, {user_profile['username']} ({user_profile['role']})")
        st.info("This dashboard gives staff a live operational view of members, circulation, stock, and overdue activity.")
        summary = system.get_dashboard_summary()
        cols = st.columns(5)
        values = [
            ("Members", summary["total_members"], "Active accounts"),
            ("Books", summary["total_books"], "Catalog items"),
            ("Loans", summary["active_loans"], "Currently checked out"),
            ("Overdue", summary["overdue_count"], "Needs attention"),
            ("Fine revenue", f"${summary['fine_revenue']:.2f}", "Collected"),
        ]
        for col, (label, value, delta) in zip(cols, values):
            with col:
                render_metric_card(label, str(value), delta)

        st.subheader("Recent notifications")
        notifications_df = notification_table_df(system)
        if not notifications_df.empty:
            st.dataframe(notifications_df.sort_values(by="timestamp", ascending=False).head(10), use_container_width=True)
        else:
            st.info("No notifications to display.")

        col_a, col_b = st.columns(2)
        with col_a:
            st.subheader("Active loans")
            st.dataframe(loan_table_df(system)[loan_table_df(system)["status"] == "ACTIVE"], use_container_width=True)
        with col_b:
            st.subheader("Overdue summary")
            overdue_df = pd.DataFrame(system.get_overdue_loans())
            if not overdue_df.empty:
                st.dataframe(overdue_df, use_container_width=True)
            else:
                st.success("No overdue books at the moment.")
    else:
        member = user_profile["member"]
        st.subheader(f"Welcome back, {member.name}!")
        st.info("Your personal library dashboard shows your current loans, upcoming due dates, reservations, and notifications.")
        member_loans = [loan for loan in system.loans if loan["member_id"] == member.id]
        active_loans = [loan for loan in member_loans if loan["status"] == "ACTIVE"]
        overdue = [loan for loan in active_loans if loan["due_date"] < date.today()]
        my_notifications = [n for n in system.notifications if n.member_id == member.id]
        cols = st.columns(4)
        with cols[0]:
            render_metric_card("Active loans", str(len(active_loans)), "This month")
        with cols[1]:
            render_metric_card("Overdue", str(len(overdue)), "Needs action")
        with cols[2]:
            render_metric_card("Borrow limit", f"{len(member.borrowed_books)}/{member.borrow_limit}", "Books out")
        with cols[3]:
            render_metric_card("Unread alerts", str(sum(1 for n in my_notifications if not n.is_read)), "Notifications")

        col_a, col_b = st.columns(2)
        with col_a:
            st.subheader("My member details")
            profile_df = pd.DataFrame([member.to_dict()])
            st.dataframe(profile_df, use_container_width=True)
        with col_b:
            st.subheader("My notifications")
            if my_notifications:
                st.dataframe(pd.DataFrame([n.to_dict() for n in my_notifications[-5:]])[['notification_type', 'message', 'timestamp']], use_container_width=True)
            else:
                st.info("No notifications for your account.")

        st.subheader("My borrowing history")
        if member_loans:
            loan_df = pd.DataFrame(member_loans)
            if "due_date" in loan_df.columns:
                loan_df["due_date"] = loan_df["due_date"].map(lambda v: v.isoformat() if hasattr(v, "isoformat") else v)
            if "checkout_date" in loan_df.columns:
                loan_df["checkout_date"] = loan_df["checkout_date"].map(lambda v: v.isoformat() if hasattr(v, "isoformat") else v)
            st.dataframe(loan_df, use_container_width=True)
        else:
            st.info("No borrowing history yet.")

if user_profile["is_staff"]:
    with catalog_tab:
        st.subheader("Member management")
        st.caption("Add, update, or review library members and their borrowing profiles.")
        member_col, member_update_col = st.columns(2)
        with member_col:
            with st.form("new_member_form"):
                member_id = st.text_input("Member ID")
                member_name = st.text_input("Member name")
                member_email = st.text_input("Email address")
                member_category = st.selectbox("Membership type", ["STUDENT", "FACULTY", "PREMIUM"])
                new_member_submitted = st.form_submit_button("Add member")
            if new_member_submitted:
                try:
                    member_class = {
                        "STUDENT": StudentMember,
                        "FACULTY": FacultyMember,
                        "PREMIUM": PremiumMember,
                    }[member_category]
                    member = member_class(member_id, member_name, member_email)
                    system.add_member(member)
                    st.success(f"Member {member_id} added successfully.")
                    st.toast(f"Added {member_name}.")
                except Exception as exc:  # pragma: no cover - UI error handling
                    st.error(f"Unable to add member: {exc}")
                    st.toast(f"Member add failed: {exc}")

        with member_update_col:
            member_choices = list(system.members.keys())
            selected_member_id = st.selectbox("Edit existing member", member_choices, index=0 if member_choices else None)
            if selected_member_id:
                member = system.get_member(selected_member_id)
                with st.form("update_member_form"):
                    updated_name = st.text_input("Name", value=member.name)
                    updated_email = st.text_input("Email", value=member.email)
                    updated_category = st.selectbox(
                        "Membership type",
                        ["STUDENT", "FACULTY", "PREMIUM"],
                        index=["STUDENT", "FACULTY", "PREMIUM"].index(member.membership_type),
                    )
                    member_update_submitted = st.form_submit_button("Update member")
                if member_update_submitted:
                    try:
                        member.name = updated_name
                        member.email = updated_email
                        member.membership_type = updated_category
                        st.success(f"Member {selected_member_id} updated.")
                        st.toast("Member updated.")
                    except Exception as exc:  # pragma: no cover
                        st.error(f"Unable to update member: {exc}")
                        st.toast(f"Update failed: {exc}")

        st.markdown("---")
        st.subheader("Book catalog management")
        st.caption("Track inventory, borrowing rules, and available copies across all library resources.")
        book_search = st.text_input("Search books by title, author, ISBN, or category", value=st.session_state.get("search_query", ""))
        book_col, book_update_col = st.columns(2)
        with book_col:
            with st.form("new_book_form"):
                isbn = st.text_input("ISBN")
                title = st.text_input("Title")
                author = st.text_input("Author")
                category = st.text_input("Category")
                total_copies = st.number_input("Total copies", min_value=1, step=1)
                book_type = st.selectbox("Book type", ["REGULAR", "REFERENCE", "DIGITAL"])
                book_submit = st.form_submit_button("Add book")
            if book_submit:
                try:
                    if book_type == "REGULAR":
                        book = RegularBook(isbn, title, author, category, total_copies)
                    elif book_type == "REFERENCE":
                        book = ReferenceBook(isbn, title, author, category, total_copies)
                    else:
                        book = DigitalResource(isbn, title, author, category, total_copies)
                    system.add_book(book)
                    st.success(f"Book {title} added successfully.")
                    st.toast(f"Added {title}.")
                except Exception as exc:  # pragma: no cover
                    st.error(f"Unable to add book: {exc}")
                    st.toast(f"Book add failed: {exc}")

        with book_update_col:
            book_choices = [book.isbn for book in system.inventory.all_books()]
            selected_isbn = st.selectbox("Edit existing book", book_choices, index=0 if book_choices else None)
            if selected_isbn:
                selected_book = system.get_book(selected_isbn)
                with st.form("update_book_form"):
                    updated_title = st.text_input("Title", value=selected_book.title)
                    updated_author = st.text_input("Author", value=selected_book.author)
                    updated_category = st.text_input("Category", value=selected_book.category)
                    updated_copies = st.number_input("Total copies", min_value=1, value=selected_book.total_copies, step=1)
                    update_book_submit = st.form_submit_button("Update book")
                if update_book_submit:
                    try:
                        selected_book.title = updated_title
                        selected_book.author = updated_author
                        selected_book.category = updated_category
                        selected_book.total_copies = updated_copies
                        selected_book.available_copies = min(selected_book.available_copies, updated_copies)
                        st.success(f"Book {selected_isbn} updated.")
                        st.toast("Book updated.")
                    except Exception as exc:  # pragma: no cover
                        st.error(f"Unable to update book: {exc}")
                        st.toast(f"Book update failed: {exc}")

        filtered_books = system.inventory.search_books(book_search) if book_search else system.inventory.all_books()
        book_df = pd.DataFrame([book.to_dict() for book in filtered_books])
        if not book_df.empty:
            book_df["status"] = book_df.apply(lambda row: "Available" if row["available_copies"] > 0 else "Out of stock", axis=1)
            book_df["status_badge"] = book_df.apply(
                lambda row: "✅ Available" if row["circulable"] and row["available_copies"] > 0 else "🚫 Unavailable" if not row["circulable"] else "📦 Out of stock",
                axis=1,
            )
        else:
            book_df = pd.DataFrame(columns=["isbn", "title", "author", "category", "available_copies", "total_copies", "circulable", "loan_period_days", "status", "status_badge"])
        st.dataframe(book_df, use_container_width=True)

        st.subheader("Member directory")
        member_search = st.text_input("Search members by ID or name", value="")
        member_matches = []
        for member in system.members.values():
            haystack = f"{member.id} {member.name} {member.email} {member.membership_type}".lower()
            if not member_search or member_search.lower() in haystack:
                member_matches.append(member)
        st.dataframe(member_table_df(system) if not member_search else pd.DataFrame([member.to_dict() for member in member_matches]), use_container_width=True)

    with circulation_tab:
        st.subheader("Issue and return books")
        st.caption("Use the desk controls to process borrow and return requests, with validation and fine checks.")
        issue_col, return_col = st.columns(2)
        with issue_col:
            with st.form("issue_form"):
                issued_member_id = st.selectbox("Member ID", list(system.members.keys()))
                issued_isbn = st.selectbox("Book ISBN", [book.isbn for book in system.inventory.all_books()])
                issue_submit = st.form_submit_button("Issue book")
            if issue_submit:
                try:
                    result = system.issue_book(issued_member_id, issued_isbn)
                    st.success(f"Issued {issued_isbn} to {issued_member_id}.")
                    st.toast(f"Issued successfully: {result['loan_id']}")
                except (
                    BookUnavailableException,
                    BorrowLimitExceededException,
                    MemberNotFoundException,
                    ReferenceOnlyItemException,
                ) as exc:
                    st.error(str(exc))
                    st.toast(str(exc))

        with return_col:
            with st.form("return_form"):
                returned_member_id = st.selectbox("Member ID", list(system.members.keys()))
                returned_isbn = st.selectbox("Book ISBN", [loan["isbn"] for loan in system.loans if loan["status"] == "ACTIVE" and loan["member_id"] == returned_member_id])
                return_submit = st.form_submit_button("Return book")
            if return_submit:
                try:
                    result = system.return_book(returned_member_id, returned_isbn)
                    st.success(f"Returned {returned_isbn} from {returned_member_id}. Fine: ${result['fine_amount']:.2f}")
                    st.toast(f"Returned {returned_isbn}.")
                except Exception as exc:
                    st.error(str(exc))
                    st.toast(str(exc))

        st.markdown("---")
        st.subheader("Active circulation ledger")
        loans_df = loan_table_df(system)
        if not loans_df.empty:
            display_loans = loans_df.copy()
            display_loans["due_date"] = display_loans["due_date"].map(lambda value: value.isoformat() if hasattr(value, "isoformat") else value)
            display_loans["checkout_date"] = display_loans["checkout_date"].map(lambda value: value.isoformat() if hasattr(value, "isoformat") else value)
            st.dataframe(display_loans, use_container_width=True)
        else:
            st.info("No active loans recorded yet.")

    with reservations_tab:
        st.subheader("Waitlist and reservation queue")
        st.caption("FIFO reservations ensure fairness when popular books are temporarily unavailable.")
        reservation_col, queue_col = st.columns(2)
        with reservation_col:
            with st.form("reservation_form"):
                reservation_member_id = st.selectbox("Member ID", list(system.members.keys()))
                reservation_isbn = st.selectbox("Book ISBN", [book.isbn for book in system.inventory.all_books()])
                reserve_submit = st.form_submit_button("Reserve book")
            if reserve_submit:
                try:
                    reservation = system.reserve_book(reservation_member_id, reservation_isbn)
                    st.success(f"Reservation queued: {reservation.reservation_id}")
                    st.toast("Reservation added.")
                except (BookUnavailableException, DuplicateReservationException, MemberNotFoundException) as exc:
                    st.error(str(exc))
                    st.toast(str(exc))

        with queue_col:
            st.subheader("Waitlist queue")
            st.dataframe(reservation_table_df(system), use_container_width=True)

        st.markdown("---")
        st.subheader("Notifications")
        st.dataframe(notification_table_df(system), use_container_width=True)

    with reports_tab:
        st.subheader("Library dashboard analytics")
        summary = system.get_dashboard_summary()
        c1, c2, c3 = st.columns(3)
        with c1:
            st.metric("Inventory utilization", f"{summary['inventory_utilization']}%")
        with c2:
            st.metric("Fine revenue", f"${summary['fine_revenue']:.2f}")
        with c3:
            st.metric("Unread notifications", sum(1 for item in system.notifications if not item.is_read))

        st.markdown("---")
        st.subheader("Book category distribution")
        category_df = pd.DataFrame([book.to_dict() for book in system.inventory.all_books()])
        if not category_df.empty:
            chart_df = category_df.groupby("category", as_index=False)["total_copies"].sum()
            st.bar_chart(chart_df.set_index("category"))
        else:
            st.info("No books available for analytics.")

        st.subheader("Fine revenue ledger")
        if system.fine_ledger:
            revenue_df = pd.DataFrame(system.fine_ledger)
            st.dataframe(revenue_df, use_container_width=True)
        else:
            st.info("No fine records yet.")

        st.subheader("Inventory utilization by title")
        inventory_df = pd.DataFrame([book.to_dict() for book in system.inventory.all_books()])
        if not inventory_df.empty:
            inventory_df["usage_pct"] = inventory_df.apply(
                lambda row: 100 if row["total_copies"] == 0 else round((row["total_copies"] - row["available_copies"]) / row["total_copies"] * 100, 2),
                axis=1,
            )
            st.dataframe(inventory_df[["title", "available_copies", "total_copies", "usage_pct"]], use_container_width=True)

        st.subheader("Recent overdue books")
        overdue_df = pd.DataFrame(system.get_overdue_loans())
        if not overdue_df.empty:
            st.dataframe(overdue_df, use_container_width=True)
        else:
            st.success("No overdue books found.")
else:
    with my_loans_tab:
        member = user_profile["member"]
        st.subheader(f"{member.name}'s borrowing activity")
        st.caption("Track your active loans, due dates, and any fines that need attention.")
        member_loans = [loan for loan in system.loans if loan["member_id"] == member.id]
        if member_loans:
            loan_df = pd.DataFrame(member_loans)
            if "due_date" in loan_df.columns:
                loan_df["due_date"] = loan_df["due_date"].map(lambda v: v.isoformat() if hasattr(v, "isoformat") else v)
            if "checkout_date" in loan_df.columns:
                loan_df["checkout_date"] = loan_df["checkout_date"].map(lambda v: v.isoformat() if hasattr(v, "isoformat") else v)
            st.dataframe(loan_df, use_container_width=True)
        else:
            st.info("You currently have no borrowing records.")

        active_books = [loan["isbn"] for loan in member_loans if loan["status"] == "ACTIVE"]
        if active_books:
            with st.form("member_return_form"):
                selected_return_isbn = st.selectbox("Return a borrowed book", options=active_books)
                submit_return = st.form_submit_button("Return selected book")
            if submit_return:
                try:
                    result = system.return_book(member.id, selected_return_isbn)
                    st.success(f"Book returned successfully. Fine: ${result['fine_amount']:.2f}")
                    st.toast("Return processed.")
                    st.rerun()
                except Exception as exc:
                    st.error(str(exc))
                    st.toast(str(exc))

    with my_reservations_tab:
        member = user_profile["member"]
        st.subheader("My reservations")
        st.caption("Check upcoming ready-to-collect books and create new reservations.")
        member_reservations = [r for r in system.reservations if r.member_id == member.id]
        if member_reservations:
            st.dataframe(pd.DataFrame([r.to_dict() for r in member_reservations]), use_container_width=True)
        else:
            st.info("You do not have any reservations yet.")

        with st.form("member_reserve_form"):
            reserve_isbn = st.selectbox("Choose a book to reserve", [book.isbn for book in system.inventory.all_books()])
            reserve_submit = st.form_submit_button("Place reservation")
        if reserve_submit:
            try:
                reservation = system.reserve_book(member.id, reserve_isbn)
                st.success(f"Reservation queued successfully: {reservation.reservation_id}")
                st.toast("Reservation created.")
                st.rerun()
            except Exception as exc:
                st.error(str(exc))
                st.toast(str(exc))

    with reports_tab:
        member = user_profile["member"]
        st.subheader(f"{member.name}'s personal report")
        st.caption("A summary of your required actions, current loans, and borrowing cost.")
        active_loans = [loan for loan in system.loans if loan["member_id"] == member.id and loan["status"] == "ACTIVE"]
        overdue_loans = [loan for loan in active_loans if loan["due_date"] < date.today()]
        fine_total = round(sum(loan.get("fine_amount", 0.0) for loan in system.loans if loan["member_id"] == member.id and loan.get("fine_amount") is not None), 2)
        c1, c2, c3 = st.columns(3)
        with c1:
            st.metric("Active loans", len(active_loans))
        with c2:
            st.metric("Overdue books", len(overdue_loans))
        with c3:
            st.metric("Total fines", f"${fine_total:.2f}")

        st.subheader("My books")
        if active_loans:
            active_df = pd.DataFrame(active_loans)
            active_df["due_date"] = active_df["due_date"].map(lambda v: v.isoformat() if hasattr(v, "isoformat") else v)
            active_df["checkout_date"] = active_df["checkout_date"].map(lambda v: v.isoformat() if hasattr(v, "isoformat") else v)
            st.dataframe(active_df, use_container_width=True)
        else:
            st.info("You currently do not have any active books checked out.")

        st.subheader("My notifications")
        member_notifications = [n for n in system.notifications if n.member_id == member.id]
        if member_notifications:
            st.dataframe(pd.DataFrame([n.to_dict() for n in member_notifications]), use_container_width=True)
        else:
            st.success("No notifications for your account.")
