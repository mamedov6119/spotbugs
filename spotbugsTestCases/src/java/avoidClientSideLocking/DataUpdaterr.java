package avoidClientSideLocking;

import java.util.HashMap;
import java.util.Map;

class GoodClientSideLockingMap1 {
    private final Map<String, Integer> dataMap = new HashMap<>();

    public Map<String, Integer> getDataMap() {
        return dataMap;
    }

    public void updateData(String key, int value) {
        dataMap.put(key, value);
    }
}

public class DataUpdaterr  {

    private final GoodClientSideLockingMap1 data;

    public DataUpdaterr(GoodClientSideLockingMap1 data) {
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