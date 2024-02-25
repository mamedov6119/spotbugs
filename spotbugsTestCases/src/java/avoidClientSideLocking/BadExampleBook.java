package avoidClientSideLocking;

import java.util.Calendar;

public class BadExampleBook {
    // Client
    private Bok book;

    BadExampleBook(Bok book) {
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
        synchronized (localCalendar) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
            }
        }
    }
}
