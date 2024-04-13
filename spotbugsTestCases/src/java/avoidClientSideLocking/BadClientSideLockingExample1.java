package avoidClientSideLocking;

import java.util.Calendar;

public class BadClientSideLockingExample1 {
    private Book book;

    BadClientSideLockingExample1(Book book) {
        this.book = book;
    }

    public synchronized void issue(int days) {
        book.issue(days);
    }

    public synchronized Calendar getDueDate() {
        return book.getDueDate();
    }

    public void renew() {
        Calendar AUE = Calendar.getInstance();
        synchronized (AUE) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
            }
        }
    }

    public void testing() {
        Calendar aaaa = Calendar.getInstance();
        synchronized (aaaa) {
            book.issue(14);
        }
    }

    public void testing2() {
        Calendar LocalSynchVar = Calendar.getInstance();
        synchronized (LocalSynchVar) {
            book.issue(14);
        }
    }

    public void justLocal() {
        Book NotLocalSynchVar = new Book("badBook");
    }
}
