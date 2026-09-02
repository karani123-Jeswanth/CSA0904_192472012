package model;
public final class ReferenceBook extends PrintedBook {
    public ReferenceBook(String isbn,String title,String author){super(isbn,title,author);}
    @Override public String getBookType(){return "REFERENCE";} @Override public boolean isReferenceOnly(){return true;}
}
