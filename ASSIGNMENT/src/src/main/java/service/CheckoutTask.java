package service;
import exception.BookUnavailableException; import exception.OverdueLimitExceededException; import model.Loan;
public final class CheckoutTask implements Runnable {
    private final LibraryService service; private final String memberId,isbn; private volatile Loan result; private volatile Exception error;
    public CheckoutTask(LibraryService service,String memberId,String isbn){this.service=service;this.memberId=memberId;this.isbn=isbn;}
    @Override public void run(){try{result=service.checkout(memberId,isbn);}catch(BookUnavailableException|OverdueLimitExceededException|RuntimeException e){error=e;}}
    public Loan getResult(){return result;} public Exception getError(){return error;}
}
