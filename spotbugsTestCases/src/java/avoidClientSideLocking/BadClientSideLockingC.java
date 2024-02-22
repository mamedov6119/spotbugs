package avoidClientSideLocking;

public class BadClientSideLockingC {
    private Object lockObject;

    public BadClientSideLockingC() {
        this.lockObject = new Object();
    }

    public void someMethod() {
        synchronized (lockObject) {
            lockObject = new Object(); 
        }
    }
}
