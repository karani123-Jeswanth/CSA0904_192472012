package model;
public final class EBook extends Book {
    public EBook(String isbn,String title,String author){super(isbn,title,author,14);}
    @Override public String getBookType(){return "EBOOK";}
}
