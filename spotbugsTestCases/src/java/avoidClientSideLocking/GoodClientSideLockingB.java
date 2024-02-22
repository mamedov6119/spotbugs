package avoidClientSideLocking;

import java.util.Calendar;
import avoidClientSideLocking.Book;

public class GoodClientSideLockingB {
    private Book book;

    GoodClientSideLockingB(Book book) {
        this.book = book;
    }

    public synchronized void issue(int days) {
        book.issue(days);
    }

    public synchronized Calendar getDueDate() {
        return book.getDueDate();
    }

    public void renew() {
        synchronized (book) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
            }
        }
    }
}
