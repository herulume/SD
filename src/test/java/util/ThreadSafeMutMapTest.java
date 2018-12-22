package util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThreadSafeMutMapTest {

    @Test
    public void putLocked() throws InterruptedException{
        ThreadSafeMutMap<String, Mutable> map = new ThreadSafeMutMap<>();
        map.putLocked("2", new Mutable(2));
        Thread t = new Thread(() -> {
            Mutable two = map.putLocked("2", new Mutable(3));
            two.incrementState();
        });
        Mutable two = map.getLocked("2");
        t.start();
        Thread.sleep(10);
        Assert.assertEquals(2, two.state);
        two.unlock();
        Thread.sleep(10);
        Assert.assertEquals(3, two.state);
    }

    @Test
    public void remove(){
    }

    @Test
    public void putAll(){
    }

    @Test
    public void clear(){
    }

    @Test
    public void valuesLocked() throws InterruptedException{
        int i = 0;
        ThreadSafeMutMap<String,Mutable> map = new ThreadSafeMutMap<>();
        System.out.println(i++);
        for(int i1 = 0; i1 < 10; i1++){
            Mutable m = new Mutable(i1);
            map.putLocked(m.getState() + "", m);
            System.out.println(m);
        }
        System.out.println(i++);
        Thread t = new Thread(() -> {
            Mutable m = map.getLocked("2");
            m.incrementState();
            m.unlock();
        });
        System.out.println(i++);
        Collection<Mutable> mutables = map.valuesLocked();
        System.out.println(i++);
        t.start();
        System.out.println(i++);
        Thread.sleep(100);
        System.out.println(i++);
        Assert.assertEquals(
                IntStream.range(0, 10).collect(HashSet::new, HashSet::add, HashSet::addAll),
                mutables.stream().map(Mutable::getState).collect(Collectors.toSet())
        );
        System.out.println(i++);
        mutables.forEach(Lockable::unlock);
        System.out.println(i++);
        t.join();
        System.out.println(i);
    }

    @Test
    public void entrySetLocked() throws InterruptedException{
        ThreadSafeMutMap<String,Mutable> map = new ThreadSafeMutMap<>();
        IntStream.range(0, 10).forEach(i -> map.putLocked(i + "", new Mutable(i)));
        Thread t = new Thread(() -> {
            Mutable m = map.getLocked("2");
            m.incrementState();
            m.unlock();
        });
        Set<Map.Entry<String,Mutable>> mutables = map.entrySetLocked();
        t.start();
        Thread.sleep(100);
        Assert.assertEquals(
                IntStream.range(0, 10).collect(HashSet::new, HashSet::add, HashSet::addAll),
                mutables.stream().map(Map.Entry::getValue).map(Mutable::getState).collect(Collectors.toSet())
        );
        mutables.stream().map(Map.Entry::getValue).forEach(Lockable::unlock);
        t.join();
    }

    private class Mutable implements Lockable {

        private int state;
        private ReentrantLock lock;

        private Mutable(int state){
            this.state = state;
            this.lock = new ReentrantLock();
        }

        private void setState(int state){
            this.state = state;
        }

        private int getState(){
            return this.state;
        }

        private void incrementState(){
            this.state++;
        }

        @Override
        public String toString(){
            return this.state + "";
        }

        @Override
        public void lock(){
            this.lock.lock();
        }

        @Override
        public void unlock(){
            this.lock.unlock();
        }
    }
}
