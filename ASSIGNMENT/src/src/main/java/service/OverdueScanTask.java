package service;
public final class OverdueScanTask implements Runnable {
    private final LibraryService service; private volatile int scanned;
    public OverdueScanTask(LibraryService service){this.service=service;}
    @Override public void run(){scanned=service.scanOverdue();}
    public int getScanned(){return scanned;}
}
