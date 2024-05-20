package avoidClientSideLocking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BadSynchDataStructures1 {
    private final Map<String, String> map;
    private final AtomicInteger atomicInt;
    private Map<Integer, String> synchMap;
    private List<Integer> list;
    
    public BadSynchDataStructures1() {
        this.map = new ConcurrentHashMap<>();
        this.atomicInt = new AtomicInteger(0);
        this.synchMap = Collections.synchronizedMap(new HashMap<>());
        this.list = new ArrayList<>();
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

    private void updateAndPrintData3() {
        synchronized (list) {
            synchMap.put(1, "value");
            System.out.println("synch on synchronized map");
        }
    }

    private void updateAndPrintData4() {
        list.add(null);
        System.out.println("synch on this");
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
