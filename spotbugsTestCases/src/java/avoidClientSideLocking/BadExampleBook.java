package avoidClientSideLocking;

import java.util.Calendar;
import avoidClientSideLocking.Book;

public class BadExampleBook {
    // Client
    private Book book;

    BadExampleBook(Book book) {
        this.book = book;
    }

    public void issue(int days) {
        book.issue(days);
    }

    public Calendar getDueDate() {
        return book.getDueDate();
    }

    public void renew() {
        Calendar localCalendar = Calendar.getInstance();
        int i = 0;
        synchronized (localCalendar) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
            }
        }
    }
}
