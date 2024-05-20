package avoidClientSideLocking;

import java.util.HashMap;
import java.util.Map;

interface DataContainer {
    Map<String, Integer> getDataMap();
    void updateData(String key, int value);
}

public class BadClientSideLockingMap2 implements DataContainer {
    private final Map<String, Integer> dataMap = new HashMap<>();

    @Override
    public Map<String, Integer> getDataMap() {
        return dataMap;
    }

    @Override
    public void updateData(String key, int value) {
        dataMap.put(key, value);
    }
}

class DataUpdaterUsingInterface {
    private final DataContainer dataContainer;

    public DataUpdaterUsingInterface(DataContainer dataContainer) {
        this.dataContainer = dataContainer;
    }

    public void updateAndPrintData(String key, int value) {
        // Error: if DataContainer changes its locking strategy in the future, this client code may break
        synchronized (dataContainer.getDataMap()) {
            dataContainer.updateData(key, value);
            System.out.println(dataContainer.getDataMap());
        }
    }
}
