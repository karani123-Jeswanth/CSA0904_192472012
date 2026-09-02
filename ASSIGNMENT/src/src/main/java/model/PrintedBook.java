package model;
public class PrintedBook extends Book {
    public PrintedBook(String isbn,String title,String author){super(isbn,title,author,21);}
    @Override public String getBookType(){return "PRINTED";}
}
