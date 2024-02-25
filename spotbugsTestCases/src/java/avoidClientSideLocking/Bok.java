package avoidClientSideLocking;

import java.util.Calendar;

public class Bok {
  // Could change its locking policy in the future
  // to use private final locks
  private Calendar dateIssued;
  private Calendar dateDue;
 
  Bok() {
    dateIssued = null;
    dateDue = null;
  }
  public synchronized void issue(int days) {
    dateIssued = Calendar.getInstance();
    dateDue = (Calendar) dateIssued.clone(); // Defensive copy to avoid exposing internal representation
    dateDue.add(Calendar.DATE, days); // Use Calendar.DATE field instead of direct access
}

    public synchronized Calendar getDueDate() {
        return (Calendar) dateDue.clone(); // Defensive copy to avoid exposing internal representation
    }

    // public synchronized void hartoshhka(int days) {
    //   dateIssued = Calendar.getInstance();
    //   dateDue = (Calendar) dateIssued.clone(); // Defensive copy to avoid exposing internal representation
    //   dateDue.add(Calendar.DATE, days); // Use Calendar.DATE field instead of direct access
    // }
}
