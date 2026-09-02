package model;
import java.time.LocalDateTime;
public final class Notification {
    private final String memberId; private final String message; private final LocalDateTime createdAt=LocalDateTime.now(); private boolean read;
    public Notification(String memberId,String message){this.memberId=memberId;this.message=message;} public String getMemberId(){return memberId;} public String getMessage(){return message;} public LocalDateTime getCreatedAt(){return createdAt;} public boolean isRead(){return read;} public void markRead(){read=true;}
    @Override public String toString(){return createdAt+" "+message+(read?" [read]":"");}
}
