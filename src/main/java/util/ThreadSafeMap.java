package util;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeMap<K, V> implements Map<K, V>, Lockable {

    final HashMap<K, V> map;
    final ReadWriteLock lock;

    public ThreadSafeMap() {
        this.map = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public int size() {
        try {
            this.lock.readLock().lock();
            return this.map.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public boolean containsKey(Object o) {
        try {
            this.lock.readLock().lock();
            return this.map.containsKey(o);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean containsValue(Object o) {
        try {
            this.lock.readLock().lock();
            return this.map.containsValue(o);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public V get(Object o) {
        try {
            this.lock.readLock().lock();
            return this.map.get(o);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public Collection<V> get(Collection<K> keys) {
        try {
            this.lock.readLock().lock();
            List<V> result = new ArrayList<>();
            for (K key : keys) {
                result.add(this.map.get(key));
            }
            return result;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public V put(K k, V v) {
        try {
            this.lock.writeLock().lock();
            return this.map.put(k, v);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public V remove(Object o) {
        try {
            this.lock.writeLock().lock();
            return this.map.remove(o);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        try {
            this.lock.writeLock().lock();
            this.map.putAll(map);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        try {
            this.lock.writeLock().lock();
            this.map.clear();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        try {
            this.lock.readLock().lock();
            return new HashSet<>(this.map.keySet());
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public Collection<V> values() {
        try {
            this.lock.readLock().lock();
            return new ArrayList<>(this.map.values());
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        try {
            this.lock.readLock().lock();
            return new HashSet<>(this.map.entrySet());
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        try {
            this.lock.readLock().lock();
            return this.map.equals(o);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void lock() {
        this.lock.writeLock().lock();
    }

    @Override
    public void unlock() {
        this.lock.writeLock().unlock();
    }
}
