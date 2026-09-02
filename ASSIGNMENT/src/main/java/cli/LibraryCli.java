package cli;

import exception.*; import model.*; import service.*;
import java.util.*;

/** ANSI console adapter; all input is validated and failures return to the menu. */
public final class LibraryCli {
    private static final String RESET="\u001B[0m", CYAN="\u001B[36m", GREEN="\u001B[32m", RED="\u001B[31m";
    private final LibraryService service; private final Scanner scanner;
    public LibraryCli(LibraryService service, Scanner scanner){this.service=service;this.scanner=scanner;}
    public void run(){
        System.out.println(CYAN+"\n=== Smart Library Management ==="+RESET);
        boolean running=true; while(running){printMenu();String choice=read("Select: "); try {switch(choice){
            case "1": addMember(); break; case "2": addBook(); break; case "3": checkout(); break; case "4": returnBook(); break;
            case "5": reserve(); break; case "6": cancel(); break;             case "7": listBooks(); break; case "8": listMembers(); break; case "9": showLoans(); break;
            case "10": showNotifications(); break; case "11": scan(); break; case "12": viewWaitlist(); break;
            case "13": reports(); break; case "14": dashboard(); break; case "0": running=false; break;
            default: System.out.println(RED+"Unknown option"+RESET);
        }} catch(RuntimeException e){System.out.println(RED+e.getMessage()+RESET);} }
    }
    private void printMenu(){System.out.println("\n1 Register member  2 Add book  3 Issue book  4 Return book  5 Place reservation  6 Cancel reservation");System.out.println("7 Display stock  8 Members  9 Active loans  10 Notifications  11 Overdue scan  12 View waitlist  13 Reports  14 Dashboard  0 Exit");}
    private String read(String prompt){System.out.print(prompt);return scanner.hasNextLine()?scanner.nextLine().trim():"0";}
    private void addMember(){String id=read("Member id: "),name=read("Name: "),email=read("Email: "),type=read("Type (S/F): ");Member m="F".equalsIgnoreCase(type)||"FACULTY".equalsIgnoreCase(type)?new FacultyMember(id,name,email):new StudentMember(id,name,email);service.registerMember(m);System.out.println(GREEN+"Registered "+m+RESET);}
    private void addBook(){String isbn=read("ISBN: "),title=read("Title: "),author=read("Author: "),type=read("Type (P/E/R): ");Book b="E".equalsIgnoreCase(type)?new EBook(isbn,title,author):"R".equalsIgnoreCase(type)?new ReferenceBook(isbn,title,author):new PrintedBook(isbn,title,author);service.addBook(b);System.out.println(GREEN+"Added "+b+RESET);}
    private void checkout(){CheckoutTask task=new CheckoutTask(service,read("Member id: "),read("ISBN: "));Thread t=new Thread(task,"checkout");t.setPriority(Thread.NORM_PRIORITY);t.start();join(t);if(task.getError()!=null)System.out.println(RED+task.getError().getMessage()+RESET);else System.out.println(GREEN+"Checked out; due "+task.getResult().getDueDate()+RESET);}
    private void returnBook(){ReturnTask task=new ReturnTask(service,read("Member id: "),read("ISBN: "));Thread t=new Thread(task,"return");t.setPriority(Thread.MIN_PRIORITY);t.start();join(t);if(task.getError()!=null)System.out.println(RED+task.getError().getMessage()+RESET);else System.out.println(GREEN+"Returned; fine = "+String.format(Locale.US,"%.2f",task.getFine())+RESET);}
    private void reserve(){try{service.reserve(read("Member id: "),read("ISBN: "));System.out.println(GREEN+"Reservation added"+RESET);}catch(DuplicateReservationException|BookUnavailableException e){System.out.println(RED+e.getMessage()+RESET);}}
    private void cancel(){System.out.println(service.cancelReservation(read("Member id: "),read("ISBN: "))?GREEN+"Reservation cancelled"+RESET:RED+"Reservation not found"+RESET);}
    private void listBooks(){for(Book b:service.books())System.out.println(b+" available="+service.isAvailable(b.getIsbn())+" reservations="+service.reservations(b.getIsbn()).size());}
    private void listMembers(){for(Member m:service.members())System.out.println(m);}
    private void showLoans(){for(Loan l:service.activeLoans())System.out.println(l.getIsbn()+" -> "+l.getMemberId()+" due "+l.getDueDate()+(l.isOverdue()?" OVERDUE":""));}
    private void showNotifications(){String id=read("Member id: ");for(Notification n:service.notifications(id))System.out.println(n);}
    private void scan(){OverdueScanTask task=new OverdueScanTask(service);Thread t=new Thread(task,"overdue-scan-now");t.setPriority(Thread.MIN_PRIORITY);t.start();join(t);System.out.println("Notifications generated: "+task.getScanned());}
    private void viewWaitlist(){String isbn=read("ISBN: ");List<Reservation> queue=service.reservations(isbn);if(queue.isEmpty())System.out.println("No reservations.");else for(int i=0;i<queue.size();i++)System.out.println((i+1)+". "+queue.get(i).getMemberId()+" at "+queue.get(i).getReservedAt());}
    private void reports(){System.out.println(service.inventoryReport());System.out.println(service.circulationReport());}
    private void dashboard(){System.out.println(CYAN+service.status()+RESET);}
    private void join(Thread t){try{t.join();}catch(InterruptedException e){Thread.currentThread().interrupt();System.out.println(RED+"Interrupted"+RESET);}}
}
