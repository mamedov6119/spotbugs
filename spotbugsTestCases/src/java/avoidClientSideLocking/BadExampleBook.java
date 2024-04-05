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

    public void justLocal() {
        Book bb = new Book("badBook");
    }
}
