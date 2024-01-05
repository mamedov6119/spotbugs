package avoidClientSideLocking;

import java.util.Calendar;
import avoidClientSideLocking.Book;

public class BadClientSideLockingBook {
    // Client
    private Book book;

    BadClientSideLockingBook(Book book) {
        this.book = book;
    }

    public void issue(int days) {
        book.issue(days);
    }

    public Calendar getDueDate() {
        return book.getDueDate();
    }

    public void фыв() {
        // Error: it may lead to issues if the underlying locking policy of the Book
        // class changes in the future
        synchronized (book) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
            }
        }
    }

}
