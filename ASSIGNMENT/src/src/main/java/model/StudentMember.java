package model;
public final class StudentMember extends Member {
    public StudentMember(String id, String name, String email) { super(id, name, email); }
    @Override public int getBorrowLimit() { return 3; }
    @Override public double getDailyFineRate() { return 1.00; }
    @Override public String getMemberType() { return "STUDENT"; }
}
