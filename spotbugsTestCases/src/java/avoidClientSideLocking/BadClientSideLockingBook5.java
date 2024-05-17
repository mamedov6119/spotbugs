package avoidClientSideLocking;

import java.util.Calendar;


public class BadClientSideLockingBook5 {
    // Client
    private Book book;

    BadClientSideLockingBook5(Book book) {
        this.book = book;
    }

    public synchronized void issue(int days) {
        book.issue(days);
    }

    public synchronized Calendar getDueDate() {
        return book.getDueDate();
    }

    public void renew() {
        SynchObj AUE = new SynchObj();
        synchronized (AUE) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
            }
        }
    }

    public void testing() {
        SynchObj aaaa = new SynchObj();
        synchronized (aaaa) {
            book.issue(14);
        }
    }

    public void justLocal() {
        Book bb = new Book("badBook");
    }
}