package main;

import cli.LibraryCli; import model.*; import server.LibraryHttpServer; import service.*;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) throws Exception {
        LibraryService service=new LibraryService(); seed(service);
        LibraryHttpServer http=null; boolean cli=true;
        for(int i=0;i<args.length;i++){if("--no-cli".equals(args[i]))cli=false; if("--server".equals(args[i])){int port=i+1<args.length?Integer.parseInt(args[++i]):8080;http=new LibraryHttpServer(service);http.start(port);}}
        Thread scanner=new Thread(new OverdueScanLoop(service),"overdue-background");scanner.setDaemon(true);scanner.setPriority(Thread.MIN_PRIORITY);scanner.start();
        if(cli)new LibraryCli(service,new Scanner(System.in)).run(); if(http!=null&&!cli){System.out.println("Press Ctrl+C to stop the server.");Thread.currentThread().join();}
    }
    private static void seed(LibraryService s){
        s.registerMember(new StudentMember("S100","Aisha Patel","aisha.patel@example.com"));
        s.registerMember(new StudentMember("S101","Noah Williams","noah.williams@example.com"));
        s.registerMember(new StudentMember("S102","Maya Chen","maya.chen@example.com"));
        s.registerMember(new StudentMember("S103","Ethan Brown","ethan.brown@example.com"));
        s.registerMember(new FacultyMember("F100","Dr. Sofia Garcia","sofia.garcia@example.com"));
        s.registerMember(new FacultyMember("F101","Dr. Liam Thompson","liam.thompson@example.com"));
        s.addBook(new PrintedBook("978-0132350884","Clean Code","Robert C. Martin"));
        s.addBook(new PrintedBook("978-0134685991","Effective Java","Joshua Bloch"));
        s.addBook(new PrintedBook("978-1491950357","Designing Data-Intensive Applications","Martin Kleppmann"));
        s.addBook(new EBook("978-0135166307","Effective Java, 3rd Edition","Joshua Bloch"));
        s.addBook(new EBook("978-0135957059","Java: The Complete Reference","Herbert Schildt"));
        s.addBook(new PrintedBook("978-1617294945","Spring in Action","Craig Walls"));
        s.addBook(new ReferenceBook("978-0198611868","The Oxford English Dictionary","Oxford Reference Desk"));
        s.addBook(new ReferenceBook("978-0262033848","Introduction to Algorithms","MIT Reference Collection"));
    }
    private static final class OverdueScanLoop implements Runnable {private final LibraryService service;OverdueScanLoop(LibraryService s){service=s;}public void run(){while(!Thread.currentThread().isInterrupted()){try{Thread.sleep(60000L);service.scanOverdue();}catch(InterruptedException e){Thread.currentThread().interrupt();}}}}
}
