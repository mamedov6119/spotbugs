package avoidClientSideLocking;

import java.util.Calendar;

public class BadExampleBook {
    // Client
    private Book book;

    BadExampleBook(Book book) {
        this.book = book;
    }

    public synchronized void issue(int days) {
        book.issue(days);
    }

    public synchronized Calendar getDueDate() {
        return book.getDueDate();
    }

    public void renew() {
        Calendar localCalendar = Calendar.getInstance();
        Calendar AUE = Calendar.getInstance();
        Book baba = new Book("asdasdas");
        synchronized (AUE) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
            }
        }
    }
}
