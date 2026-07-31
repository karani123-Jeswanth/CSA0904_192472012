import java.util.Scanner;

public class LibraryBookManagement {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Book[] books = new Book[10];
        int count = 0, choice;

        do {
            System.out.println("\n1.Add  2.Display  3.Issue  4.Return  5.Search  6.Exit");
            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    if (count < books.length) {
                        System.out.print("Book ID: ");
                        int id = sc.nextInt();
                        sc.nextLine();
                        System.out.print("Title: ");
                        String title = sc.nextLine();
                        System.out.print("Author: ");
                        String author = sc.nextLine();
                        System.out.print("Price: ");
                        double price = sc.nextDouble();
                        books[count++] = new Book(id, title, author, price);
                        System.out.println("Book added successfully.");
                    }
                    break;

                case 2:
                    for (int i = 0; i < count; i++) books[i].display();
                    break;

                case 3:
                    System.out.print("Enter Book ID: ");
                    int issueId = sc.nextInt();
                    for (int i = 0; i < count; i++)
                        if (books[i].getBookId() == issueId) books[i].issueBook();
                    break;

                case 4:
                    System.out.print("Enter Book ID: ");
                    int returnId = sc.nextInt();
                    for (int i = 0; i < count; i++)
                        if (books[i].getBookId() == returnId) books[i].returnBook();
                    break;

                case 5:
                    System.out.print("Enter Book ID: ");
                    int searchId = sc.nextInt();
                    for (int i = 0; i < count; i++)
                        if (books[i].getBookId() == searchId) books[i].display();
                    break;
            }
        } while (choice != 6);
        sc.close();
    }
}

class Book {
    private int bookId;
    private String title;
    private String author;
    private double price;
    private boolean issued;

    Book(int bookId, String title, String author, double price) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.price = price;
        this.issued = false;
    }

    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public double getPrice() { return price; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setPrice(double price) { this.price = price; }

    public void issueBook() {
        if (!issued) {
            issued = true;
            System.out.println("Book issued successfully.");
        } else {
            System.out.println("Book is already issued.");
        }
    }

    public void returnBook() {
        if (issued) {
            issued = false;
            System.out.println("Book returned successfully.");
        } else {
            System.out.println("Book was not issued.");
        }
    }

    public void display() {
        System.out.println(bookId + " | " + title + " | " + author +
                " | $" + price + " | " + (issued ? "Issued" : "Available"));
    }
}

