package avoidClientSideLocking;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestCodeChecker {
    
    private final Map<String, String> map;
    
    public TestCodeChecker() {
        this.map = new ConcurrentHashMap<>();
    }

    private void updateAndPrintData() {
        synchronized (map) {
            map.put("key", "value");
            System.out.println("synch on concurrent map");
        }
    }

}
