package model;

import java.util.Objects;

/** Common member state; subclasses provide policy values through polymorphism. */
public abstract class Member {
    private final String id;
    private String name;
    private String email;
    protected Member(String id, String name, String email) {
        this.id = require(id, "Member id"); this.name = require(name, "Member name"); this.email = require(email, "Email");
        if (!email.contains("@")) throw new IllegalArgumentException("Email must contain '@'");
    }
    private static String require(String value, String label) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required"); return value.trim(); }
    public final String getId() { return id; }
    public final String getName() { return name; }
    public final String getEmail() { return email; }
    public final void setName(String name) { this.name = require(name, "Member name"); }
    public final void setEmail(String email) {
        String validated = require(email, "Email");
        if (!validated.contains("@")) throw new IllegalArgumentException("Email must contain '@'");
        this.email = validated;
    }
    public abstract int getBorrowLimit();
    public abstract double getDailyFineRate();
    public abstract String getMemberType();
    @Override public boolean equals(Object other) { return other instanceof Member && id.equals(((Member) other).id); }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return getMemberType()+"{"+id+", "+name+", limit="+getBorrowLimit()+"}"; }
}
