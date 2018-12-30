package util;


import java.util.*;

public class ThreadSafeMutMap<K, V extends Lockable> extends ThreadSafeMap<K, V> {


    public ThreadSafeMutMap() {
        super();
    }

    public V getLocked(K o) {
        V v = this.map.get(o);
        if (v == null) return null;
        v.lock();
        return v;
    }

    public Collection<V> getLocked(Collection<K> keys) {
        try {
            this.lock.readLock().lock();
            List<V> result = new ArrayList<>();
            for (K key : keys) {
                V v = this.map.get(key);
                v.lock();
                result.add(v);
            }
            return result;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public V putLocked(K k, V v) {
        this.lock.writeLock().lock();
        try {
            if (this.map.containsKey(k)) this.map.get(k).lock();
            return this.map.put(k, v);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public V remove(Object o) {
        this.lock.writeLock().lock();
        if (this.map.containsKey(o)) {
            this.map.get(o).lock();
            V r = this.map.remove(o);
            r.unlock();
            this.lock.writeLock().unlock();
            return r;
        } else {
            this.lock.writeLock().unlock();
            return null;
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        this.lock.writeLock().lock();
        map.keySet().forEach(this::remove);
        this.map.putAll(map);
        this.lock.writeLock().unlock();
    }

    @Override
    public void clear() {
        this.lock.writeLock().lock();
        this.map.values().forEach(Lockable::lock);
        for (Iterator<V> iterator = this.map.values().iterator(); iterator.hasNext(); ) {
            V v = iterator.next();
            iterator.remove();
            v.unlock();
        }
        this.lock.writeLock().unlock();
    }

    public Collection<V> valuesLocked() {
        this.lock.writeLock().lock();
        this.map.values().forEach(Lockable::lock);
        Collection<V> r = new ArrayList<>(this.map.values());
        this.lock.writeLock().unlock();
        return r;
    }

    public Set<Entry<K, V>> entrySetLocked() {
        this.lock.writeLock().lock();
        this.map.values().forEach(Lockable::lock);
        HashSet<Entry<K, V>> r = new HashSet<>(this.map.entrySet());
        this.lock.writeLock().unlock();
        return r;
    }

    @Override
    public String toString() {
        return this.map.toString();
    }

    @Override
    public V get(Object o) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use getLocked instead");
    }

    @Override
    public Collection<V> get(Collection<K> keys) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use getLocked instead");
    }

    @Override
    public V put(K k, V v) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use putLocked instead");
    }

    @Override
    public Collection<V> values() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Use valuesLocked instead");
    }

    @Override
    public Set<Entry<K, V>> entrySet() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("User entrySetLocked instead");
    }
}
