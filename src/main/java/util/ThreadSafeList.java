package util;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeList <T> implements List<T> {

    private List<T> list;
    private ReadWriteLock lock;

    public ThreadSafeList(){
        this.list = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public int size(){
        try{
            this.lock.readLock().lock();
            return this.list.size();
        }finally{
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty(){
        return this.size() == 0;
    }

    @Override
    public boolean contains(Object o){
        try{
            this.lock.readLock().lock();
            return this.list.contains(o);
        }finally{
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean add(T t){
        try{
            this.lock.writeLock().lock();
            return this.list.add(t);
        }finally{
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public boolean remove(Object o){
        try{
            this.lock.writeLock().lock();
            return this.list.remove(o);
        }finally{
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public boolean containsAll(Collection<?> collection){
        try{
            this.lock.readLock().lock();
            return this.list.containsAll(collection);
        }finally{
            this.lock.readLock().unlock();
        }
    }


    @Override
    public boolean addAll(Collection<? extends T> collection){
        try{
            this.lock.writeLock().lock();
            return this.list.addAll(collection);
        }finally{
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public boolean addAll(int i, Collection<? extends T> collection){
        try{
            this.lock.writeLock().lock();
            return this.list.addAll(i, collection);
        }finally{
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public boolean removeAll(Collection<?> collection){
        try{
            this.lock.writeLock().lock();
            return this.list.removeAll(collection);
        }finally{
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public boolean retainAll(Collection<?> collection){
        try{
            this.lock.writeLock().lock();
            return this.list.retainAll(collection);
        }finally{
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public void clear(){
        try{
            this.lock.writeLock().lock();
            this.list.clear();
        }finally{
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public T get(int i){
        try{
            this.lock.readLock().lock();
            return this.list.get(i);
        }finally{
            this.lock.readLock().unlock();
        }
    }


    @Override
    public T set(int i, T t){
        try{
            this.lock.writeLock().lock();
            return this.list.set(i, t);
        }finally{
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public void add(int i, T t){
        try{
            this.lock.writeLock().lock();
            this.list.add(i, t);
        }finally{
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public T remove(int i){
        try{
            this.lock.writeLock().lock();
            return this.list.remove(i);
        }finally{
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public int indexOf(Object o){
        try{
            this.lock.readLock().lock();
            return this.list.indexOf(o);
        }finally{
            this.lock.readLock().unlock();
        }
    }

    @Override
    public int lastIndexOf(Object o){
        try{
            this.lock.readLock().lock();
            return this.list.lastIndexOf(o);
        }finally{
            this.lock.readLock().unlock();
        }
    }

    @Override
    public Iterator<T> iterator() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T1> T1[] toArray(T1[] t1s) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }


    @Override
    public ListIterator<T> listIterator() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator(int i) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> subList(int i, int i1) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
