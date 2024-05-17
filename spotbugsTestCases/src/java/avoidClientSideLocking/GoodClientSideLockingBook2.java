package avoidClientSideLocking;

import java.util.Calendar;

public final class GoodClientSideLockingBook2 {
    private Book book;
    private final Object lock = new Object();

    GoodClientSideLockingBook2(Book book) {
        this.book = book;
    }

    public void issue(int days) {
        synchronized (lock) {
            book.issue(days);
        }
    }

    public Calendar getDueDate() {
        synchronized (lock) {
            return book.getDueDate();
        }
    }

    public void renew() {
        synchronized (lock) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
            }
        }
    }
}