package avoidClientSideLocking;

import java.util.Calendar;
import avoidClientSideLocking.Bok;

public class BadClientSideLockingBookC {
    // Client
    private Bok book;

    BadClientSideLockingBookC(Bok book) {
        this.book = book;
    }

    public void issue(int days) {
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
