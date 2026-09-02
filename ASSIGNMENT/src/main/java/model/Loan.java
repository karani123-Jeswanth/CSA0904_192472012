package model;
import java.time.LocalDate; import java.time.temporal.ChronoUnit;
public final class Loan {
    private final String memberId; private final String isbn; private final LocalDate checkoutDate; private final LocalDate dueDate;
    public Loan(String memberId,Book book){this.memberId=memberId;this.isbn=book.getIsbn();this.checkoutDate=LocalDate.now();this.dueDate=checkoutDate.plusDays(book.getLoanDays());}
    public String getMemberId(){return memberId;} public String getIsbn(){return isbn;} public LocalDate getCheckoutDate(){return checkoutDate;} public LocalDate getDueDate(){return dueDate;}
    public long overdueDays(){return Math.max(0,ChronoUnit.DAYS.between(dueDate,LocalDate.now()));} public boolean isOverdue(){return overdueDays()>0;}
}
