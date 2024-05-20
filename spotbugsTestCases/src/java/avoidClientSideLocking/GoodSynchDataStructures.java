package avoidClientSideLocking;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GoodSynchDataStructures {
    
    private final Map<String, String> map;
    private final AtomicInteger atomicInt;
    private Map<Integer, String> synchMap;
    
    public GoodSynchDataStructures() {
        this.map = new ConcurrentHashMap<>();
        this.atomicInt = new AtomicInteger(0);
        this.synchMap = Collections.synchronizedMap(new HashMap<>());
        
    }

    private void updateAndPrintData() {
        synchronized (map) {
            map.put("key", "value");
            System.out.println("synch on concurrent map");
        }
    }

    private void updateAndPrintData2() {
        map.put(null, null);
        System.out.println("synch on concurrent map");
    }

    private void updateAndPrintData5() {
        synchronized (atomicInt) {
            System.out.println("synch on this");
        }
    }

    private void updateAndPrintData6() {
        atomicInt.incrementAndGet();
        System.out.println("synch on atomic int");
    }

    private void updateAndPrintData7() {
        synchronized (synchMap) {
            synchMap.put(1, "value");
            System.out.println("synch on synchronized map");
        }
    }

    private void updateAndPrintData8() {
        synchMap.put(1, "value");
        System.out.println("synch on synchronized map");
    }

}
