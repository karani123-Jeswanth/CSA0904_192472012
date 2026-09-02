package model;
import java.time.LocalDateTime; import java.util.Objects;
public final class Reservation implements Comparable<Reservation> {
    private final String memberId; private final String isbn; private final LocalDateTime reservedAt;
    public Reservation(String memberId,String isbn){if(memberId==null||memberId.trim().isEmpty()||isbn==null||isbn.trim().isEmpty()) throw new IllegalArgumentException("Reservation member and ISBN are required"); this.memberId=memberId.trim();this.isbn=isbn.trim();this.reservedAt=LocalDateTime.now();}
    public String getMemberId(){return memberId;} public String getIsbn(){return isbn;} public LocalDateTime getReservedAt(){return reservedAt;}
    @Override public int compareTo(Reservation other){int r=reservedAt.compareTo(other.reservedAt); return r==0?memberId.compareTo(other.memberId):r;}
    @Override public boolean equals(Object o){if(!(o instanceof Reservation))return false; Reservation r=(Reservation)o; return memberId.equals(r.memberId)&&isbn.equals(r.isbn);} @Override public int hashCode(){return Objects.hash(memberId,isbn);}
}
