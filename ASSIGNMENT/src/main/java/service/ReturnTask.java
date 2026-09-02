package service;
import exception.BookUnavailableException;
public final class ReturnTask implements Runnable {
    private final LibraryService service; private final String memberId,isbn; private volatile Double fine; private volatile Exception error;
    public ReturnTask(LibraryService service,String memberId,String isbn){this.service=service;this.memberId=memberId;this.isbn=isbn;}
    @Override public void run(){try{fine=service.returnBook(memberId,isbn);}catch(BookUnavailableException|RuntimeException e){error=e;}}
    public Double getFine(){return fine;} public Exception getError(){return error;}
}
