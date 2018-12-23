package util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class ThreadSafeMapTest {

    private Map<String,Integer> testCase;

    public ThreadSafeMapTest(){
        this.testCase = new HashMap<>();
        IntStream.range(0, 10000).forEach(i -> this.testCase.put(i + "", i));
    }

    @Test
    public void size(){
        ThreadSafeMap<String,Integer> list = new ThreadSafeMap<>();
        ArrayList<Integer> values = IntStream.range(0, 10000).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        int chunkSize = this.testCase.size() / 10;
        int from = 0, to = chunkSize;
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Adder(values.subList(from, to), list)));
            threads.get(threads.size() - 1).start();
            from = to;
            to = to + chunkSize;
        }
        for(Thread thread : threads){
            try{
                thread.join();
            }catch(InterruptedException ignored){
            }
        }
        Assert.assertEquals(this.testCase.size(), list.size());
    }

    @Test
    public void containsKey(){
        ThreadSafeMap<String,Integer> map = new ThreadSafeMap<>();
        ArrayList<Integer> values = IntStream.range(0, 10000).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        int chunkSize = this.testCase.size() / 10;
        int from = 0, to = chunkSize;
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Adder(values.subList(from, to), map)));
            threads.get(threads.size() - 1).start();
            from = to;
            to = to + chunkSize;
        }
        for(Thread thread : threads){
            try{
                thread.join();
            }catch(InterruptedException ignored){
            }
        }
        for(String key : this.testCase.keySet()){
            Assert.assertTrue(map.containsKey(key));
        }
    }

    @Test
    public void containsValue(){
        ThreadSafeMap<String,Integer> map = new ThreadSafeMap<>();
        ArrayList<Integer> values = IntStream.range(0, 10000).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        int chunkSize = this.testCase.size() / 10;
        int from = 0, to = chunkSize;
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Adder(values.subList(from, to), map)));
            threads.get(threads.size() - 1).start();
            from = to;
            to = to + chunkSize;
        }
        for(Thread thread : threads){
            try{
                thread.join();
            }catch(InterruptedException ignored){
            }
        }
        for(Integer value : this.testCase.values()){
            Assert.assertTrue(map.containsValue(value));
        }
    }

    @Test
    public void get(){
        ThreadSafeMap<String,Integer> map = new ThreadSafeMap<>();
        ArrayList<Integer> values = IntStream.range(0, 10000).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        int chunkSize = this.testCase.size() / 10;
        int from = 0, to = chunkSize;
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Adder(values.subList(from, to), map)));
            threads.get(threads.size() - 1).start();
            from = to;
            to = to + chunkSize;
        }
        for(int i = 0; i < 10000; i++){
            Assert.assertTrue(map.get(i + "") == null || map.get(i + "") == i);
        }
        for(Thread thread : threads){
            try{
                thread.join();
            }catch(InterruptedException ignored){
            }
        }
    }

    @Test
    public void remove(){
        ThreadSafeMap<String,Integer> map = new ThreadSafeMap<>();
        ArrayList<Integer> values = IntStream.range(0, 10000).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        values.forEach(i -> map.put(i.toString(), i));
        int chunkSize = this.testCase.size() / 10;
        int from = 0, to = chunkSize;
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Remover(values.subList(from, to), map)));
            threads.get(threads.size() - 1).start();
            from = to;
            to = to + chunkSize;
        }
        for(Thread thread : threads){
            try{
                thread.join();
            }catch(InterruptedException ignored){
            }
        }
        Assert.assertTrue(map.isEmpty());
    }

    private class Adder implements Runnable {

        private final List<Integer> integers;
        private final ThreadSafeMap<String,Integer> map;

        private Adder(List<Integer> integers, ThreadSafeMap<String,Integer> map){
            this.integers = integers;
            this.map = map;
        }

        @Override
        public void run(){
            for(Integer integer : integers){
                this.map.put(integer.toString(), integer);
            }
        }
    }

    private class Remover implements Runnable {

        private final List<Integer> integers;
        private final ThreadSafeMap<String,Integer> map;

        private Remover(List<Integer> integers, ThreadSafeMap<String,Integer> map){
            this.integers = integers;
            this.map = map;
        }

        @Override
        public void run(){
            for(Integer i : integers){
                this.map.remove(i.toString());
            }
        }
    }
}
