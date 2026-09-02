package model;
import java.util.Objects;
public abstract class Book {
    private final String isbn;     private String title; private String author; private final int loanDays;
    protected Book(String isbn, String title, String author, int loanDays) {
        this.isbn=require(isbn,"ISBN"); this.title=require(title,"Title"); this.author=require(author,"Author");
        if (loanDays < 1) throw new IllegalArgumentException("Loan days must be positive"); this.loanDays=loanDays;
    }
    private static String require(String value,String label) { if(value==null||value.trim().isEmpty()) throw new IllegalArgumentException(label+" is required"); return value.trim(); }
    public final String getIsbn(){return isbn;} public final String getTitle(){return title;} public final String getAuthor(){return author;} public final int getLoanDays(){return loanDays;}
    public final void setTitle(String title){this.title=require(title,"Title");}
    public final void setAuthor(String author){this.author=require(author,"Author");}
    public abstract String getBookType(); public boolean isReferenceOnly(){return false;}
    @Override public boolean equals(Object other){return other instanceof Book && isbn.equals(((Book)other).isbn);} @Override public int hashCode(){return Objects.hash(isbn);}
    @Override public String toString(){return getBookType()+"{"+isbn+", "+title+"}";}
}
