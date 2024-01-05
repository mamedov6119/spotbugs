package avoidClientSideLocking;

import java.util.HashMap;
import java.util.Map;

public class BadClientSideLockingMap {
    private final Map<String, Integer> dataMap = new HashMap<>();

    public Map<String, Integer> getDataMap() {
        return dataMap;
    }

    public void updateData(String key, int value) {
        dataMap.put(key, value);
    }
}

class DataUpdater {
    public void фыв(BadClientSideLockingMap obj, String key, int value) {
        //Error: if BadClientSideLockingMap changes its locking strategy in the future, this client code may break
        synchronized (obj.getDataMap()) {
            obj.updateData(key, value);
            System.out.println(obj.getDataMap());
        }
    }
}