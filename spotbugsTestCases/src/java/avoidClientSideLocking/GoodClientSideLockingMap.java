package avoidClientSideLocking;

import java.util.HashMap;
import java.util.Map;

public class GoodClientSideLockingMap {
    private final Map<String, Integer> dataMap = new HashMap<>();

    public Map<String, Integer> getDataMap() {
        return dataMap;
    }

    public void updateData(String key, int value) {
        synchronized (dataMap) {
            dataMap.put(key, value);
        }
    }
}

class DataUpdaterr {
    public void updateAndPrintData(GoodClientSideLockingMap obj, String key, int value) {
        synchronized (obj.getDataMap()) {
            obj.updateData(key, value);
            System.out.println(obj.getDataMap());
        }
    }
}