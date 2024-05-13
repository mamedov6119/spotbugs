package avoidClientSideLocking;

import java.util.HashMap;
import java.util.Map;

class DataUpdaterr {
    private final Map<String, Integer> dataMap = new HashMap<>();

    public Map<String, Integer> getDataMap() {
        return dataMap;
    }

    public void updateData(String key, int value) {
        dataMap.put(key, value);
    }
}

public class GoodClientSideLockingMap1  {

    private final DataUpdaterr data;

    public GoodClientSideLockingMap1(DataUpdaterr data) {
        this.data = data;
    }

    public synchronized void addData(String key, int value) {
        data.updateData(key, value);
    }

    public synchronized void updateAndPrintData(String key, int value) {
        addData(key, value);
        System.out.println(data.getDataMap());
    }
}