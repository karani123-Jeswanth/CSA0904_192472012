package model;
public final class FacultyMember extends Member {
    public FacultyMember(String id, String name, String email) { super(id, name, email); }
    @Override public int getBorrowLimit() { return 8; }
    @Override public double getDailyFineRate() { return 0.50; }
    @Override public String getMemberType() { return "FACULTY"; }
}
