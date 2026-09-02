package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import model.Book;
import model.EBook;
import model.FacultyMember;
import model.Loan;
import model.Member;
import model.PrintedBook;
import model.ReferenceBook;
import model.Reservation;
import model.StudentMember;
import service.LibraryService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Zero-dependency dashboard server. Handlers only read thread-safe service snapshots.
 * The small request parser intentionally supports the flat objects used by the dashboard.
 */
public final class LibraryHttpServer {
    private static final int MAX_BODY_BYTES = 32 * 1024;
    private final LibraryService service;
    private HttpServer server;

    public LibraryHttpServer(LibraryService service) {
        this.service = service;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::dashboard);
        server.createContext("/api/status", this::status);
        server.createContext("/api/books", this::books);
        server.createContext("/api/members", this::members);
        server.createContext("/api/loans", this::loans);
        server.createContext("/api/reservations", this::reservations);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Dashboard: http://localhost:" + port + "/");
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void dashboard(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8", page());
    }

    private void status(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            methodNotAllowed(exchange, "GET");
            return;
        }
        sendJson(exchange, 200, "{\"members\":" + service.memberCount()
            + ",\"books\":" + service.bookCount()
            + ",\"activeLoans\":" + service.loanCount()
            + ",\"availableBooks\":" + availableBooks() + "}");
    }

    private void books(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Book book : service.books()) {
                if (!first) json.append(',');
                first = false;
                json.append("{\"isbn\":\"").append(esc(book.getIsbn()))
                    .append("\",\"title\":\"").append(esc(book.getTitle()))
                    .append("\",\"author\":\"").append(esc(book.getAuthor()))
                    .append("\",\"type\":\"").append(esc(book.getBookType()))
                    .append("\",\"loanDays\":").append(book.getLoanDays())
                    .append(",\"referenceOnly\":").append(book.isReferenceOnly())
                    .append(",\"available\":").append(service.isAvailable(book.getIsbn())).append('}');
            }
            json.append(']');
            sendJson(exchange, 200, json.toString());
            return;
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                Map<String, String> data = body(exchange);
                String type = valueOrDefault(data, "type", valueOrDefault(data, "bookType", "PRINTED")).toUpperCase(Locale.ROOT);
                Book book;
                if ("EBOOK".equals(type) || "E-BOOK".equals(type) || "E_BOOK".equals(type)) {
                    book = new EBook(value(data, "isbn"), value(data, "title"), value(data, "author"));
                } else if ("REFERENCE".equals(type) || "REFERENCEBOOK".equals(type)) {
                    book = new ReferenceBook(value(data, "isbn"), value(data, "title"), value(data, "author"));
                } else if ("PRINTED".equals(type) || "PRINTEDBOOK".equals(type)) {
                    book = new PrintedBook(value(data, "isbn"), value(data, "title"), value(data, "author"));
                } else {
                    throw new IllegalArgumentException("Book type must be PRINTED, EBOOK, or REFERENCE");
                }
                service.addBook(book);
                sendJson(exchange, 201, "{\"message\":\"Book added\",\"isbn\":\"" + esc(book.getIsbn()) + "\"}");
            } catch (Exception error) {
                sendError(exchange, 400, error);
            }
            return;
        }
        methodNotAllowed(exchange, "GET, POST");
    }

    private void members(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Member member : service.members()) {
                if (!first) json.append(',');
                first = false;
                json.append("{\"id\":\"").append(esc(member.getId()))
                    .append("\",\"name\":\"").append(esc(member.getName()))
                    .append("\",\"email\":\"").append(esc(member.getEmail()))
                    .append("\",\"type\":\"").append(esc(member.getMemberType()))
                    .append("\",\"borrowLimit\":").append(member.getBorrowLimit()).append('}');
            }
            json.append(']');
            sendJson(exchange, 200, json.toString());
            return;
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                Map<String, String> data = body(exchange);
                String type = valueOrDefault(data, "type", valueOrDefault(data, "memberType", "STUDENT")).toUpperCase(Locale.ROOT);
                Member member;
                if ("FACULTY".equals(type) || "FACULTYMEMBER".equals(type)) {
                    member = new FacultyMember(value(data, "id"), value(data, "name"), value(data, "email"));
                } else if ("STUDENT".equals(type) || "STUDENTMEMBER".equals(type)) {
                    member = new StudentMember(value(data, "id"), value(data, "name"), value(data, "email"));
                } else {
                    throw new IllegalArgumentException("Member type must be STUDENT or FACULTY");
                }
                service.registerMember(member);
                sendJson(exchange, 201, "{\"message\":\"Member registered\",\"id\":\"" + esc(member.getId()) + "\"}");
            } catch (Exception error) {
                sendError(exchange, 400, error);
            }
            return;
        }
        methodNotAllowed(exchange, "GET, POST");
    }

    private void loans(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Loan loan : service.activeLoans()) {
                if (!first) json.append(',');
                first = false;
                json.append("{\"isbn\":\"").append(esc(loan.getIsbn()))
                    .append("\",\"memberId\":\"").append(esc(loan.getMemberId()))
                    .append("\",\"checkoutDate\":\"").append(loan.getCheckoutDate())
                    .append("\",\"dueDate\":\"").append(loan.getDueDate()).append("\"}");
            }
            json.append(']');
            sendJson(exchange, 200, json.toString());
            return;
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                Map<String, String> data = body(exchange);
                String action = valueOrDefault(data, "action", valueOrDefault(data, "operation", "issue")).toLowerCase(Locale.ROOT);
                String path = exchange.getRequestURI().getPath().toLowerCase(Locale.ROOT);
                if (path.endsWith("/return") || path.endsWith("/checkin")) action = "return";
                if (path.endsWith("/issue") || path.endsWith("/checkout")) action = "issue";
                String memberId = value(data, "memberId", "member");
                String isbn = value(data, "isbn", "book");
                if ("return".equals(action) || "checkin".equals(action)) {
                    double fine = service.returnBook(memberId, isbn);
                    sendJson(exchange, 200, "{\"message\":\"Book returned\",\"fine\":" + fine + "}");
                } else if ("issue".equals(action) || "checkout".equals(action) || "borrow".equals(action)) {
                    Loan loan = service.checkout(memberId, isbn);
                    sendJson(exchange, 201, "{\"message\":\"Book issued\",\"memberId\":\""
                        + esc(loan.getMemberId()) + "\",\"isbn\":\"" + esc(loan.getIsbn())
                        + "\",\"dueDate\":\"" + loan.getDueDate() + "\"}");
                } else {
                    throw new IllegalArgumentException("Loan action must be issue or return");
                }
            } catch (Exception error) {
                sendError(exchange, 400, error);
            }
            return;
        }
        methodNotAllowed(exchange, "GET, POST");
    }

    private void reservations(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Book book : service.books()) {
                for (Reservation reservation : service.reservations(book.getIsbn())) {
                    if (!first) json.append(',');
                    first = false;
                    json.append("{\"isbn\":\"").append(esc(reservation.getIsbn()))
                        .append("\",\"memberId\":\"").append(esc(reservation.getMemberId()))
                        .append("\",\"reservedAt\":\"").append(reservation.getReservedAt()).append("\"}");
                }
            }
            json.append(']');
            sendJson(exchange, 200, json.toString());
            return;
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                Map<String, String> data = body(exchange);
                String memberId = value(data, "memberId", "member");
                String isbn = value(data, "isbn", "book");
                service.reserve(memberId, isbn);
                sendJson(exchange, 201, "{\"message\":\"Reservation placed\",\"memberId\":\""
                    + esc(memberId) + "\",\"isbn\":\"" + esc(isbn) + "\"}");
            } catch (Exception error) {
                sendError(exchange, 400, error);
            }
            return;
        }
        methodNotAllowed(exchange, "GET, POST");
    }

    private int availableBooks() {
        int count = 0;
        for (Book book : service.books()) if (service.isAvailable(book.getIsbn())) count++;
        return count;
    }

    private Map<String, String> body(HttpExchange exchange) throws IOException {
        long declared = exchange.getRequestHeaders().getFirst("Content-Length") == null
            ? -1L : Long.parseLong(exchange.getRequestHeaders().getFirst("Content-Length"));
        if (declared > MAX_BODY_BYTES) throw new IllegalArgumentException("Request body is too large");
        InputStream input = exchange.getRequestBody();
        ByteArrayOutputStreamEx output = new ByteArrayOutputStreamEx();
        byte[] buffer = new byte[2048];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            if (output.size() > MAX_BODY_BYTES) throw new IllegalArgumentException("Request body is too large");
        }
        String raw = new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
        if (raw.isEmpty()) throw new IllegalArgumentException("Request body is required");
        if (raw.startsWith("{")) return parseJsonObject(raw);
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], "UTF-8");
            String val = parts.length == 1 ? "" : URLDecoder.decode(parts[1], "UTF-8");
            result.put(key, val);
        }
        return result;
    }

    private static Map<String, String> parseJsonObject(String raw) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        int index = skipWhitespace(raw, 1);
        if (index < raw.length() && raw.charAt(index) == '}') return result;
        while (index < raw.length()) {
            if (raw.charAt(index) != '"') throw new IllegalArgumentException("Invalid JSON object");
            ParseValue key = quoted(raw, index);
            index = skipWhitespace(raw, key.next);
            if (index >= raw.length() || raw.charAt(index) != ':') throw new IllegalArgumentException("Invalid JSON object");
            index = skipWhitespace(raw, index + 1);
            ParseValue value;
            if (index < raw.length() && raw.charAt(index) == '"') {
                value = quoted(raw, index);
            } else {
                int end = index;
                while (end < raw.length() && raw.charAt(end) != ',' && raw.charAt(end) != '}') end++;
                value = new ParseValue(raw.substring(index, end).trim(), end);
            }
            result.put(key.value, value.value);
            index = skipWhitespace(raw, value.next);
            if (index >= raw.length()) break;
            if (raw.charAt(index) == '}') {
                if (skipWhitespace(raw, index + 1) != raw.length()) throw new IllegalArgumentException("Invalid JSON object");
                return result;
            }
            if (raw.charAt(index) != ',') throw new IllegalArgumentException("Invalid JSON object");
            index = skipWhitespace(raw, index + 1);
        }
        throw new IllegalArgumentException("Invalid JSON object");
    }

    private static ParseValue quoted(String raw, int start) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                if (c == 'n') value.append('\n');
                else if (c == 'r') value.append('\r');
                else if (c == 't') value.append('\t');
                else if (c == 'b') value.append('\b');
                else if (c == 'f') value.append('\f');
                else if (c == 'u') {
                    if (i + 4 >= raw.length()) throw new IllegalArgumentException("Invalid JSON unicode escape");
                    String hex = raw.substring(i + 1, i + 5);
                    try {
                        value.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException error) {
                        throw new IllegalArgumentException("Invalid JSON unicode escape");
                    }
                    i += 4;
                }
                else value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return new ParseValue(value.toString(), i + 1);
            } else {
                value.append(c);
            }
        }
        throw new IllegalArgumentException("Invalid JSON string");
    }

    private static int skipWhitespace(String value, int start) {
        int index = start;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) index++;
        return index;
    }

    private static String value(Map<String, String> data, String key) {
        String value = data.get(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(key + " is required");
        return value.trim();
    }

    private static String value(Map<String, String> data, String key, String fallback) {
        String value = data.get(key);
        if (value == null || value.trim().isEmpty()) value = data.get(fallback);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(key + " is required");
        return value.trim();
    }

    private static String valueOrDefault(Map<String, String> data, String key, String defaultValue) {
        String value = data.get(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private void methodNotAllowed(HttpExchange exchange, String allow) throws IOException {
        exchange.getResponseHeaders().set("Allow", allow);
        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private void sendError(HttpExchange exchange, int code, Exception error) throws IOException {
        String message = error.getMessage() == null ? "Request failed" : error.getMessage();
        sendJson(exchange, code, "{\"error\":\"" + esc(message) + "\"}");
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static void sendJson(HttpExchange exchange, int code, String body) throws IOException {
        send(exchange, code, "application/json; charset=utf-8", body);
    }

    private static void send(HttpExchange exchange, int code, String type, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(code, data.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(data);
        }
    }

    private String page() {
        StringBuilder h = new StringBuilder();
        h.append("<!doctype html><html lang='en'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>")
            .append("<title>Libra | Smart Library</title><style>")
            .append(":root{--ink:#172033;--muted:#718096;--line:#e8edf5;--brand:#635bff;--cyan:#21c7b7;--bg:#f6f8fc;--card:#fff;--danger:#d85c63}*{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:var(--bg);color:var(--ink);font:14px Inter,Segoe UI,Arial,sans-serif}aside{position:fixed;width:245px;inset:0 auto 0 0;background:#171b35;color:#fff;padding:28px 18px;box-shadow:8px 0 30px #1d24401c;z-index:2}.logo{font-size:24px;font-weight:800;padding:0 14px 34px}.logo span{color:#8e88ff}.nav{display:block;padding:13px 15px;margin:5px 0;border-radius:12px;color:#aeb6d0;text-decoration:none}.nav.active,.nav:hover{background:#ffffff14;color:#fff}.nav b{display:inline-block;width:28px;color:#8e88ff}.main{margin-left:245px;padding:28px 38px;max-width:1500px}.top{display:flex;justify-content:space-between;align-items:center;margin-bottom:30px}.eyebrow{color:var(--brand);font-weight:700;letter-spacing:.08em;text-transform:uppercase;font-size:11px}.top h1{font-size:30px;margin:7px 0 0}.pulse{background:#fff;border:1px solid var(--line);padding:11px 16px;border-radius:12px;color:var(--muted)}.pulse i{display:inline-block;width:8px;height:8px;border-radius:50%;background:var(--cyan);margin-right:8px}.cards{display:grid;grid-template-columns:repeat(4,1fr);gap:17px}.card,.panel{background:var(--card);border:1px solid var(--line);border-radius:18px;box-shadow:0 8px 25px #24304c08}.card{padding:21px}.card .label{color:var(--muted);font-size:12px;font-weight:600}.metric{font-size:30px;font-weight:800;margin-top:10px}.metric small{font-size:12px;color:var(--cyan);font-weight:700;margin-left:7px}.grid{display:grid;grid-template-columns:1.45fr 1fr;gap:20px;margin-top:22px}.panel{padding:22px}.panel-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;gap:12px}.panel h2{margin:0;font-size:17px}.panel-head a,.link{color:var(--brand);font-size:12px;font-weight:700;cursor:pointer}.search{width:210px;padding:10px 13px;border:1px solid var(--line);border-radius:10px;outline:0}.search:focus,input:focus,select:focus{border-color:var(--brand)}table{width:100%;border-collapse:collapse}th{text-align:left;color:var(--muted);font-size:11px;text-transform:uppercase;letter-spacing:.05em;padding:10px 8px;border-bottom:1px solid var(--line)}td{padding:13px 8px;border-bottom:1px solid var(--line);font-size:13px}tr:last-child td{border:0}.book{font-weight:700}.sub{color:var(--muted);font-size:11px;margin-top:3px}.tag{padding:5px 9px;border-radius:20px;font-size:11px;font-weight:700}.ok{background:#e4faf5;color:#138c77}.out{background:#fff0e8;color:#d66b34}.avatar{width:31px;height:31px;border-radius:10px;background:#ebeaff;color:var(--brand);display:inline-flex;align-items:center;justify-content:center;font-weight:800;margin-right:8px}.person{display:flex;align-items:center}.empty{padding:25px;color:var(--muted);text-align:center}.footer{text-align:center;color:#9aa4b7;font-size:11px;margin:24px}.forms{display:grid;grid-template-columns:repeat(3,1fr);gap:20px;margin-top:22px}.form-card h2{margin-bottom:5px}.form-card p{color:var(--muted);font-size:12px;margin:0 0 15px}.form-row{display:grid;grid-template-columns:1fr 1fr;gap:10px}.form-card input,.form-card select{width:100%;padding:10px 11px;border:1px solid var(--line);border-radius:9px;margin-bottom:10px;background:#fff;color:var(--ink);outline:0}.btn{border:0;border-radius:9px;padding:10px 14px;background:var(--brand);color:white;font-weight:700;cursor:pointer}.btn:hover{filter:brightness(1.08)}.btn.alt{background:#edf0ff;color:var(--brand)}.feedback{min-height:18px;margin-top:9px;font-size:12px;font-weight:600}.feedback.good{color:#138c77}.feedback.bad{color:var(--danger)}@media(max-width:1100px){.cards{grid-template-columns:repeat(2,1fr)}.forms{grid-template-columns:1fr 1fr}}@media(max-width:900px){aside{width:70px;padding:20px 10px}.logo{font-size:0;padding:0 10px 30px}.logo span{font-size:23px}.nav{font-size:0;text-align:center}.nav b{width:auto;font-size:16px}.main{margin-left:70px;padding:22px}.grid{grid-template-columns:1fr}.forms{grid-template-columns:1fr}}@media(max-width:560px){.top{display:block}.pulse{display:inline-block;margin-top:15px}.cards{grid-template-columns:1fr}.search{width:130px}td:nth-child(3),th:nth-child(3){display:none}.form-row{display:block}}</style></head><body>")
            .append("<aside><div class='logo'>Libra<span>.</span></div><a class='nav active' href='#overview'><b>◈</b> Overview</a><a class='nav' href='#books'><b>▣</b> Collection</a><a class='nav' href='#members'><b>♙</b> Members</a><a class='nav' href='#loans'><b>↗</b> Circulation</a><a class='nav' href='#reservations'><b>≡</b> Waitlist</a><a class='nav' href='#actions'><b>＋</b> Add &amp; act</a></aside><main class='main' id='overview'><div class='top'><div><div class='eyebrow'>Library operations center</div><h1>Good afternoon, Librarian</h1></div><div class='pulse'><i></i><span id='updated'>Live system connected</span></div></div><section class='cards'><div class='card'><div class='label'>TOTAL COLLECTION</div><div class='metric' id='booksMetric'>—</div><small>Curated titles</small></div><div class='card'><div class='label'>AVAILABLE NOW</div><div class='metric' id='availableMetric'>— <small>● healthy</small></div><small>Ready to borrow</small></div><div class='card'><div class='label'>ACTIVE MEMBERS</div><div class='metric' id='membersMetric'>—</div><small>Registered readers</small></div><div class='card'><div class='label'>ACTIVE LOANS</div><div class='metric' id='loansMetric'>—</div><small>Currently circulating</small></div></section>")
            .append("<div class='grid'><section class='panel' id='books'><div class='panel-head'><h2>Collection overview</h2><input id='bookSearch' class='search' placeholder='Search title, author or ISBN'></div><table><thead><tr><th>Book</th><th>Format</th><th>Status</th></tr></thead><tbody id='bookRows'></tbody></table></section><section class='panel' id='loans'><div class='panel-head'><h2>Current circulation</h2><a onclick='load()'>Refresh ↻</a></div><table><thead><tr><th>Member</th><th>Book</th><th>Due</th></tr></thead><tbody id='loanRows'></tbody></table></section></div>")
            .append("<div class='grid'><section class='panel' id='members'><div class='panel-head'><h2>Member directory</h2><span class='eyebrow'>Live registry</span></div><table><thead><tr><th>Member</th><th>Type</th><th>ID</th></tr></thead><tbody id='memberRows'></tbody></table></section><section class='panel' id='reservations'><div class='panel-head'><h2>Reservation queue</h2><span class='eyebrow'>FIFO waitlist</span></div><div id='reservationRows'></div></section></div>")
            .append("<section class='forms' id='actions'><form class='panel form-card' onsubmit='submitForm(event,this,\"/api/members\")'><h2>Register a member</h2><p>Add a student or faculty reader to the live registry.</p><div class='form-row'><input name='id' placeholder='Member ID' required><input name='name' placeholder='Full name' required></div><input name='email' type='email' placeholder='Email address' required><select name='type'><option value='STUDENT'>Student</option><option value='FACULTY'>Faculty</option></select><button class='btn'>Register member</button><div class='feedback'></div></form>")
            .append("<form class='panel form-card' onsubmit='submitForm(event,this,\"/api/books\")'><h2>Add to collection</h2><p>Catalog printed, e-book, or reference titles.</p><input name='isbn' placeholder='ISBN' required><input name='title' placeholder='Book title' required><input name='author' placeholder='Author' required><select name='type'><option value='PRINTED'>Printed book</option><option value='EBOOK'>E-book</option><option value='REFERENCE'>Reference book</option></select><button class='btn'>Add book</button><div class='feedback'></div></form>")
            .append("<div class='panel form-card'><h2>Circulation desk</h2><p>Issue, return, or reserve a title using member and ISBN details.</p><form onsubmit='submitForm(event,this,\"/api/loans\")'><input name='memberId' placeholder='Member ID' required><input name='isbn' placeholder='ISBN' required><div class='form-row'><button class='btn' name='action' value='issue'>Issue book</button><button class='btn alt' name='action' value='return'>Return book</button></div><div class='feedback'></div></form><form onsubmit='submitForm(event,this,\"/api/reservations\")' style='margin-top:15px'><input name='memberId' placeholder='Member ID' required><input name='isbn' placeholder='ISBN' required><button class='btn alt'>Place reservation</button><div class='feedback'></div></form></div></section>")
            .append("<div class='footer'>LIBRA SMART LIBRARY · Secure local operations dashboard · Auto-refreshes every 15 seconds</div></main><script>")
            .append("const $=id=>document.getElementById(id),esc=s=>String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\\\"/g,'&quot;').replace(/'/g,'&#39;');let books=[];async function json(u){const r=await fetch(u);if(!r.ok)throw Error(r.status);return r.json()}async function load(){try{const [s,b,m,l,r]=await Promise.all([json('/api/status'),json('/api/books'),json('/api/members'),json('/api/loans'),json('/api/reservations')]);books=b;$('booksMetric').textContent=s.books;$('availableMetric').textContent=s.availableBooks;$('membersMetric').textContent=s.members;$('loansMetric').textContent=s.activeLoans;$('updated').textContent='Updated '+new Date().toLocaleTimeString();renderBooks();$('loanRows').innerHTML=l.length?l.map(x=>`<tr><td><b>${esc(x.memberId)}</b></td><td>${esc(x.isbn)}</td><td>${esc(x.dueDate)}</td></tr>`).join(''):'<tr><td colspan=3 class=empty>No active loans</td></tr>';$('memberRows').innerHTML=m.length?m.map(x=>`<tr><td><div class=person><span class=avatar>${esc((x.name||'?')[0])}</span><b>${esc(x.name)}</b></div><div class=sub>${esc(x.email)}</div></td><td><span class='tag ok'>${esc(x.type)}</span></td><td>${esc(x.id)}</td></tr>`).join(''):'<tr><td colspan=3 class=empty>No members yet</td></tr>';$('reservationRows').innerHTML=r.length?r.map((x,i)=>`<div style='display:flex;justify-content:space-between;padding:14px 0;border-bottom:1px solid var(--line)'><div><b>#${i+1} ${esc(x.memberId)}</b><div class=sub>${esc(x.isbn)}</div></div><span class='tag out'>WAITING</span></div>`).join(''):'<div class=empty>All queues are clear</div>'}catch(e){$('updated').textContent='Connection issue'}}function renderBooks(){const q=($('bookSearch').value||'').toLowerCase();const rows=books.filter(x=>(x.title+x.isbn+x.type+x.author).toLowerCase().includes(q));$('bookRows').innerHTML=rows.length?rows.map(x=>`<tr><td><div class=book>${esc(x.title)}</div><div class=sub>${esc(x.author)} · ${esc(x.isbn)}</div></td><td><span class='tag ok'>${esc(x.type)}</span></td><td><span class='tag ${x.available?'ok':'out'}'>${x.available?'AVAILABLE':'CHECKED OUT'}</span></td></tr>`).join(''):'<tr><td colspan=3 class=empty>No matching books</td></tr>'}async function submitForm(event,form,url){event.preventDefault();const feedback=form.querySelector('.feedback');feedback.className='feedback';feedback.textContent='Saving…';try{const r=await fetch(url,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(new FormData(form))});const data=await r.json();if(!r.ok)throw Error(data.error||'Request failed');feedback.className='feedback good';feedback.textContent=data.message||'Saved';form.reset();await load()}catch(e){feedback.className='feedback bad';feedback.textContent=e.message}}$('bookSearch').addEventListener('input',renderBooks);document.querySelectorAll('.nav').forEach(n=>n.addEventListener('click',()=>{document.querySelectorAll('.nav').forEach(x=>x.classList.remove('active'));n.classList.add('active')}));load();setInterval(load,15000);")
            .append("</script></body></html>");
        return h.toString();
    }

    private static final class ParseValue {
        private final String value;
        private final int next;
        private ParseValue(String value, int next) {
            this.value = value;
            this.next = next;
        }
    }

    /** Small bounded output buffer, avoiding a dependency on any JSON or web library. */
    private static final class ByteArrayOutputStreamEx extends java.io.ByteArrayOutputStream {
        private static final long serialVersionUID = 1L;
    }
}
