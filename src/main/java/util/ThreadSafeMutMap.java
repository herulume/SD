package util;


import java.util.*;

public class ThreadSafeMutMap <K, V extends Lockable> extends ThreadSafeMap<K,V> {


    public ThreadSafeMutMap(){
        super();
    }

    public V getLocked(K o){
        V v = this.map.get(o);
        if(v == null) return null;
        v.lock();
        return v;
    }

    public V putLocked(K k, V v){
        this.lock.writeLock().lock();
        if(this.map.containsKey(k)){
            this.map.get(k).lock();
            V r = this.map.put(k, v);
            this.lock.writeLock().unlock();
            return r;
        }else{
            this.map.put(k, v);
            this.lock.writeLock().unlock();
            return null;
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public V remove(Object o){
        this.lock.writeLock().unlock();
        if(this.map.containsKey(o)){
            this.map.get(o).lock();
            V r = this.map.remove(o);
            r.unlock();
            this.lock.writeLock().unlock();
            return r;
        }else{
            this.lock.writeLock().unlock();
            return null;
        }
    }

    @Override
    public void putAll(Map<? extends K,? extends V> map){
        this.lock.writeLock().lock();
        map.keySet().forEach(this::remove);
        this.map.putAll(map);
        this.lock.writeLock().unlock();
    }

    @Override
    public void clear(){
        this.lock.writeLock().lock();
        this.map.values().forEach(Lockable::lock);
        this.map.keySet().forEach(k -> this.map.remove(k).unlock());
        this.lock.writeLock().unlock();
    }

    public Collection<V> valuesLocked(){
        this.lock.writeLock().lock();
        this.map.values().forEach(Lockable::lock);
        Collection<V> r = new ArrayList<>(this.map.values());
        this.lock.writeLock().unlock();
        return r;
    }

    public Set<Entry<K,V>> entrySetLocked(){
        this.lock.writeLock().lock();
        this.map.values().forEach(Lockable::lock);
        HashSet<Entry<K,V>> r = new HashSet<>(this.map.entrySet());
        this.lock.writeLock().unlock();
        return r;
    }

    @Override
    public String toString(){
        return this.map.toString();
    }

    @Override
    public V get(Object o) throws UnsupportedOperationException{
        throw new UnsupportedOperationException("Use getLocked instead");
    }

    @Override
    public V put(K k, V v) throws UnsupportedOperationException{
        throw new UnsupportedOperationException("Use putLocked instead");
    }

    @Override
    public Collection<V> values() throws UnsupportedOperationException{
        throw new UnsupportedOperationException("Use valuesLocked instead");
    }

    @Override
    public Set<Entry<K,V>> entrySet() throws UnsupportedOperationException{
        throw new UnsupportedOperationException("User entrySetLocked instead");
    }
}
