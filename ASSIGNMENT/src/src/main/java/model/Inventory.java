package model;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;
/** Thread-safe inventory; the HashSet enforces ISBN uniqueness while the concurrent map serves lookups. */
public final class Inventory {
    private final ConcurrentHashMap<String,Book> books=new ConcurrentHashMap<String,Book>(); private final HashSet<String> isbnIndex=new HashSet<String>();
    public synchronized void add(Book book){Objects.requireNonNull(book,"book"); if(!isbnIndex.add(book.getIsbn())) throw new IllegalArgumentException("Duplicate ISBN: "+book.getIsbn()); books.put(book.getIsbn(),book);}
    public Book get(String isbn){return books.get(isbn);} public boolean contains(String isbn){return books.containsKey(isbn);} public Collection<Book> all(){return Collections.unmodifiableCollection(books.values());}
    public synchronized Book remove(String isbn){Book book=books.remove(isbn);if(book!=null)isbnIndex.remove(isbn);return book;} public int size(){return books.size();}
}
