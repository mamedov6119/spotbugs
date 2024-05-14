package avoidClientSideLocking;

import java.util.Calendar;

public class BadClientSideLockingBook6 {
    // Client
    private Book book;
    private int days;
    private static Object lock = new Object();

    BadClientSideLockingBook6(Book book, int days) {
        this.book = book;
        this.days = days;
    }

    public synchronized void issue(int days) {
        book.issue(days);
    }

    public int getDueDate() {
        lock = new Object();
        return 0;
    }

    public void renew() {
        synchronized (lock) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
                days = 14;
            }
        }
    }

    public void smth() {
        System.out.println("smth");
    }
}
