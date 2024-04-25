package avoidClientSideLocking;

import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;

public abstract class Repository {
    public Date parse(String dateString) throws ParseException {
        final DateFormat format = getDateFormat();
        synchronized (format) {
            return format.parse(dateString);
        }
    }

    private DateFormat getDateFormat() {
        return new RepositoryDateFormat();
    }

    private class RepositoryDateFormat extends DateFormat {
        private static final long serialVersionUID = -6951382723884436414L;

        private final Locale locale = Locale.ENGLISH;
        // NOTE: SimpleDateFormat is not thread-safe, lock must be held when used
        
        String[] datePatterns = { "yyyy-MM-dd", "MM/dd/yyyy", "dd-MMM-yyyy" };
        private final SimpleDateFormat[] formatters = new SimpleDateFormat[datePatterns.length];

        {
            // initialize date formatters
            for (int i = 0; i < datePatterns.length; i++) {
                formatters[i] = new SimpleDateFormat(datePatterns[i], locale);
                /*
                 * TODO: the following would be nice - but currently it
                 * could break the compatibility with some repository dates
                 */
                // formatters[i].setLenient(false);
            }
        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
            throw new UnsupportedOperationException("not implemented");
        }

        @Override
        public Date parse(String source) throws ParseException {
            ParseException head = null, tail = null;
            for (SimpleDateFormat formatter : formatters) {
                try {
                    return formatter.parse(source);
                } catch (ParseException ex1) {
                    /*
                     * Adding all exceptions together to get some info in
                     * the logs.
                     */
                    ex1 = new ParseException(
                            String.format("%s with format \"%s\" and locale \"%s\"",
                                    ex1.getMessage(),
                                    formatter.toPattern(),
                                    locale),
                            ex1.getErrorOffset()
                    );
                    if (head == null) {
                        head = tail = ex1;
                    } else {
                        tail.initCause(ex1);
                        tail = ex1;
                    }
                }
            }
            throw head != null ? head : new ParseException(String.format("Unparseable date: \"%s\"", source), 0);
        }

        @Override
        public Date parse(String source, ParsePosition pos) {
            throw new UnsupportedOperationException("not implemented");
        }

}
}
