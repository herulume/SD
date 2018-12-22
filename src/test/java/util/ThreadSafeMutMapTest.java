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
        t.join();
        Assert.assertEquals(3, two.state);
    }

    @Test
    public void remove() throws InterruptedException{
        ThreadSafeMutMap<String, Mutable> map = new ThreadSafeMutMap<>();
        IntStream.range(0, 10).forEach(i -> map.putLocked(i + "", new Mutable(i)));
        Mutable v1 = map.getLocked("1");
        Thread t2 = new Thread(() -> map.remove("2"));
        t2.start();
        t2.join();
        Assert.assertTrue(!map.containsKey("2"));
        Thread t1 = new Thread(() -> map.remove("1"));
        t1.start();
        Thread.sleep(10);
        Assert.assertTrue(map.containsKey("1")); // probably a deadlock
        v1.unlock();
        t1.join();
    }

    @Test
    public void clear() throws InterruptedException{
        ThreadSafeMutMap<String, Mutable> map = new ThreadSafeMutMap<>();
        IntStream.range(0, 10).forEach(i -> map.putLocked(i + "", new Mutable(i)));
        Thread t = new Thread(map::clear);
        Mutable v = map.getLocked("1");
        t.start();
        Thread.sleep(10);
        Assert.assertNotEquals(map.size(), 0);
        v.unlock();
        Thread.sleep(10);
        Assert.assertEquals(map.size(), 0);
    }

    @Test
    public void valuesLocked() throws InterruptedException{
        ThreadSafeMutMap<String,Mutable> map = new ThreadSafeMutMap<>();
        IntStream.range(0, 10).forEach(i -> map.putLocked(i + "", new Mutable(i)));
        Thread t = new Thread(() -> {
            Mutable m = map.getLocked("2");
            m.incrementState();
            m.unlock();
        });
        Collection<Mutable> mutables = map.valuesLocked();
        t.start();
        Thread.sleep(100);
        Assert.assertEquals(
                IntStream.range(0, 10).collect(HashSet::new, HashSet::add, HashSet::addAll),
                mutables.stream().map(Mutable::getState).collect(Collectors.toSet())
        );
        mutables.forEach(Lockable::unlock);
        t.join();
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
