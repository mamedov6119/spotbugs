package avoidClientSideLocking;

import java.util.Calendar;

public class BadClientSideLockingBook {
    // Client
    private Book book;

    BadClientSideLockingBook(Book book) {
        this.book = book;
    }

    // error in the methods that use book but are not synchronized
    public void issue(int days) {
        book.issue(days);
    }

    public Calendar getDueDate() {
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

    public void hartoshhka(int days) {
        System.out.println("Hartoshka");
    }

}
