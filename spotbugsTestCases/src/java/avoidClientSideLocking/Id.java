package avoidClientSideLocking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class Id<T> implements Comparable<Id<T>> {

    private final static ConcurrentMap<Class<?>, ConcurrentMap<String, Id<?>>> cacheId = new ConcurrentHashMap<>();
    private final static ConcurrentMap<Class<?>, List<Id<?>>> cacheIndex = new ConcurrentHashMap<>();

    public static <T> Id<T> create(final String key, final Class<T> type) {
        if (key == null) {
            throw new NullPointerException("key must not be null");
        }

        ConcurrentMap<String, Id<?>> mapId = cacheId.computeIfAbsent(type, k -> new ConcurrentHashMap<>(1000));

        Id<?> id = mapId.get(key);
        synchronized (mapId) {
            id = mapId.get(key);
            if (id == null) {
                List<Id<?>> mapIndex = mapId.isEmpty() ? new ArrayList<>(1000) : cacheIndex.get(type);
                int index = mapIndex.size();
                id = new IdImpl<>(key, index);
                mapIndex.add(id);
                cacheIndex.put(type, mapIndex);
                mapId.put(key, id);
            }
        }
        return (Id<T>) id;
    }

    public abstract int index();

    private static class IdImpl<T> extends Id<T> {

        private final String id;
        private final int index;

        IdImpl(final String id, final int index) {
            this.id = id;
            this.index = index;
        }

        @Override
        public int index() {
            return this.index;
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        @Override
        public String toString() {
            return this.id;
        }

		@Override
		public int compareTo(Id<T> o) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("Unimplemented method 'compareTo'");
		}
    }
}