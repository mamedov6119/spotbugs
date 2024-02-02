package avoidClientSideLocking;

public class BadClientSideLockingExample {
    private Object lockObject = new Object();

    public void someMethod() {
        synchronized (lockObject) {
            lockObject = new Object(); 
        }
    }
}
