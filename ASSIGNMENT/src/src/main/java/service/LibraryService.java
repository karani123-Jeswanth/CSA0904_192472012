package service;

import exception.BookUnavailableException;
import exception.DuplicateReservationException;
import exception.InvalidMemberException;
import exception.OverdueLimitExceededException;
import model.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Application facade. Locks protect the compound inventory/waitlist transitions. */
public final class LibraryService {
    private final Inventory inventory = new Inventory();
    private final ConcurrentHashMap<String, Member> members = new ConcurrentHashMap<String, Member>();
    private final Hashtable<String, Member> memberLookup = new Hashtable<String, Member>();
    private final ConcurrentHashMap<String, Loan> activeLoans = new ConcurrentHashMap<String, Loan>();
    private final ArrayList<Loan> activeTransactions = new ArrayList<Loan>();
    private final HashMap<String, HashSet<String>> loansByMember = new HashMap<String, HashSet<String>>();
    private final HashMap<String, ArrayList<Reservation>> waitlists = new HashMap<String, ArrayList<Reservation>>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Notification>> notifications = new ConcurrentHashMap<String, CopyOnWriteArrayList<Notification>>();
    private final Set<String> overdueNotified = ConcurrentHashMap.newKeySet();
    private final TreeSet<String> memberIds = new TreeSet<String>();
    private final Object inventoryLock = new Object();
    private final Object waitlistLock = new Object();

    public void registerMember(Member member) {
        if (member == null) throw new InvalidMemberException("Member is required");
        synchronized (inventoryLock) {
            if (members.putIfAbsent(member.getId(), member) != null) throw new InvalidMemberException("Duplicate member id: " + member.getId());
            memberLookup.put(member.getId(), member); memberIds.add(member.getId()); loansByMember.put(member.getId(), new HashSet<String>());
        }
    }
    public void addBook(Book book) { inventory.add(book); }
    public Member findMember(String id) { return memberLookup.get(id); }
    public Book findBook(String isbn) { return inventory.get(isbn); }
    public Collection<Member> members() { return Collections.unmodifiableCollection(members.values()); }
    public Collection<Book> books() { return inventory.all(); }
    public Collection<Loan> activeLoans() { synchronized (inventoryLock) { return Collections.unmodifiableList(new ArrayList<Loan>(activeTransactions)); } }
    public List<Reservation> reservations(String isbn) { synchronized(waitlistLock) { return new ArrayList<Reservation>(waitlists.getOrDefault(isbn,new ArrayList<Reservation>())); } }
    public List<Notification> notifications(String memberId) { return new ArrayList<Notification>(notifications.getOrDefault(memberId,new CopyOnWriteArrayList<Notification>())); }

    private Member memberOrThrow(String id) { Member m=members.get(id); if(m==null) throw new InvalidMemberException("Unknown member: "+id); return m; }
    private Book bookOrThrow(String isbn) throws BookUnavailableException { Book b=inventory.get(isbn); if(b==null) throw new BookUnavailableException("Unknown ISBN: "+isbn); return b; }

    public void reserve(String memberId, String isbn) throws DuplicateReservationException, BookUnavailableException {
        memberOrThrow(memberId); bookOrThrow(isbn);
        synchronized(waitlistLock) {
            ArrayList<Reservation> queue=waitlists.computeIfAbsent(isbn,k->new ArrayList<Reservation>());
            Reservation reservation=new Reservation(memberId,isbn);
            if(queue.contains(reservation)) throw new DuplicateReservationException("Already reserved by member");
            queue.add(reservation);
            // FIFO queue is an ArrayList; append is O(1), cancellation uses ListIterator safely.
        }
    }
    public boolean cancelReservation(String memberId,String isbn) {
        synchronized(waitlistLock) {
            ArrayList<Reservation> queue=waitlists.get(isbn); if(queue==null)return false;
            ListIterator<Reservation> it=queue.listIterator(); while(it.hasNext()) if(it.next().getMemberId().equals(memberId)){it.remove();waitlistLock.notifyAll();return true;}
            return false;
        }
    }
    public Loan checkout(String memberId,String isbn) throws BookUnavailableException, OverdueLimitExceededException {
        Member member=memberOrThrow(memberId); Book book=bookOrThrow(isbn);
        if(book.isReferenceOnly()) throw new BookUnavailableException("Reference books are library-use only");
        synchronized(inventoryLock) {
            HashSet<String> memberLoans=loansByMember.get(memberId);
            if(memberLoans.size()>=member.getBorrowLimit()) throw new OverdueLimitExceededException("Borrow limit reached for "+memberId);
            if(activeLoans.containsKey(isbn)) throw new BookUnavailableException("Book is currently checked out");
            synchronized(waitlistLock) {
                ArrayList<Reservation> queue=waitlists.get(isbn);
                if(queue!=null&&!queue.isEmpty()&&!queue.get(0).getMemberId().equals(memberId)) throw new BookUnavailableException("Book is reserved for "+queue.get(0).getMemberId());
                if(queue!=null&&!queue.isEmpty()) queue.remove(0);
                Loan loan=new Loan(memberId,book); activeLoans.put(isbn,loan); activeTransactions.add(loan); memberLoans.add(isbn); return loan;
            }
        }
    }
    public double returnBook(String memberId,String isbn) throws BookUnavailableException {
        memberOrThrow(memberId);
        synchronized(inventoryLock) {
            Loan loan=activeLoans.get(isbn); if(loan==null||!loan.getMemberId().equals(memberId)) throw new BookUnavailableException("No matching active loan");
            activeLoans.remove(isbn);
            ListIterator<Loan> transactions = activeTransactions.listIterator();
            while (transactions.hasNext()) if (transactions.next().getIsbn().equals(isbn)) { transactions.remove(); break; }
            loansByMember.get(memberId).remove(isbn);
            double fine=loan.overdueDays()*memberOrThrow(memberId).getDailyFineRate();
            overdueNotified.remove(loan.getMemberId()+"|"+loan.getIsbn()+"|"+loan.getDueDate());
            synchronized(waitlistLock) {
                ArrayList<Reservation> queue = waitlists.get(isbn);
                if (queue != null && !queue.isEmpty()) {
                    notifyMember(queue.get(0).getMemberId(), "Reserved book available: " + isbn);
                }
                waitlistLock.notifyAll();
            }
            return fine;
        }
    }
    public boolean isAvailable(String isbn) { return inventory.contains(isbn)&&!activeLoans.containsKey(isbn); }
    public boolean awaitAvailable(String isbn,long timeoutMillis) throws InterruptedException {
        long end=System.currentTimeMillis()+timeoutMillis;
        synchronized(waitlistLock) { while(!isAvailable(isbn)){long remaining=end-System.currentTimeMillis();if(remaining<=0)return false;waitlistLock.wait(remaining);} return true; }
    }
    public int scanOverdue() {
        int count=0;
        // Iterator avoids concurrent modification while traversing a point-in-time copy.
        Iterator<Loan> iterator=new ArrayList<Loan>(activeLoans.values()).iterator();
        while(iterator.hasNext()){Loan loan=iterator.next();if(loan.isOverdue()){String key=loan.getMemberId()+"|"+loan.getIsbn()+"|"+loan.getDueDate();if(overdueNotified.add(key)){notifyMember(loan.getMemberId(),"Overdue: "+loan.getIsbn()+" was due "+loan.getDueDate());count++;}}}
        return count;
    }
    private void notifyMember(String memberId,String message) { notifications.computeIfAbsent(memberId,k->new CopyOnWriteArrayList<Notification>()).add(new Notification(memberId,message)); }
    public int unreadNotifications(String memberId){int n=0;for(Notification x:notifications(memberId))if(!x.isRead())n++;return n;}
    public int memberCount(){return members.size();} public int bookCount(){return inventory.size();} public int loanCount(){return activeLoans.size();}
    public String status(){return "members="+memberCount()+", books="+bookCount()+", activeLoans="+loanCount()+", date="+LocalDate.now();}
    public String inventoryReport(){
        StringBuilder report=new StringBuilder("INVENTORY REPORT\n");
        for(Book book:books()) report.append(book).append(" | available=").append(isAvailable(book.getIsbn())).append('\n');
        return report.toString();
    }
    public String circulationReport(){
        StringBuilder report=new StringBuilder("CIRCULATION REPORT\n");
        for(Loan loan:activeLoans()) report.append(loan.getIsbn()).append(" -> ").append(loan.getMemberId())
            .append(" | due=").append(loan.getDueDate()).append(" | overdue=").append(loan.isOverdue()).append('\n');
        return report.toString();
    }
}
