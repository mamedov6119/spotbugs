package avoidClientSideLocking;

import java.util.Calendar;

public class BadClientSideLockingBook {
    // Client
    private final Book book;

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
        // Error: it may lead to issues if the underlying locking policy of the Book class changes in the future
        synchronized (book) {
            if (book.getDueDate().before(Calendar.getInstance())) {
                throw new IllegalStateException("Book overdue");
            } else {
                book.issue(14); // Issue book for 14 days
            }
        }
    }

    static final class Book {
        // Could change its locking policy in the future
        // to use private final locks
        private final String title;
        private Calendar dateIssued;
        private Calendar dateDue;

        Book(String title) {
            this.title = title;
        }

        public synchronized void issue(int days) {
            dateIssued = Calendar.getInstance();
            dateDue = Calendar.getInstance();
            dateDue.add(dateIssued.DATE, days);
        }

        public synchronized Calendar getDueDate() {
            return dateDue;
        }
    }

}
