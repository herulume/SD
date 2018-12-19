package util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ThreadSafeListTest {

    private List<Integer> testCase;

    public ThreadSafeListTest(){
        List<Integer> l = IntStream.range(0, 10000).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        this.testCase = new ArrayList<>(l);
    }

    @Test
    public void size(){
        ThreadSafeList<Integer> list = new ThreadSafeList<>();
        int chunkSize = this.testCase.size() / 10;
        int from = 0, to = chunkSize;
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Pusher(this.testCase.subList(from, to), list)));
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
    public void isEmpty(){
    }

    @Test
    public void contains(){
        ThreadSafeList<Integer> list = new ThreadSafeList<>();
        int chunkSize = this.testCase.size() / 10;
        int from = 0, to = chunkSize;
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Pusher(this.testCase.subList(from, to), list)));
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
        for(int i = 0; i < list.size(); i++){
            Assert.assertTrue(this.testCase.contains(i));
        }
    }

    @Test
    public void add(){
        ThreadSafeList<Integer> list = new ThreadSafeList<>();
        int chunkSize = this.testCase.size() / 10;
        int from = 0, to = chunkSize;
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Pusher(this.testCase.subList(from, to), list)));
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
        //can't compare the lists because the order of the elements might differ
        Assert.assertEquals(this.testCase.size(), list.size());
    }

    @Test
    public void remove(){
        ThreadSafeList<Integer> list = new ThreadSafeList<>();
        list.addAll(this.testCase);
        int chunkSize = this.testCase.size() / 10;
        int from = 0, to = chunkSize;
        List<Thread> threads = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            threads.add(new Thread(new Popper(to - from, list)));
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
        Assert.assertTrue(list.isEmpty());
    }

    private class Pusher implements Runnable {

        private List<Integer> values;
        private ThreadSafeList<Integer> list;

        Pusher(List<Integer> values, ThreadSafeList<Integer> list){
            this.values = values;
            this.list = list;
        }

        @Override
        public void run(){
            for(Integer value : values){
                //noinspection UseBulkOperation
                this.list.add(value);
            }
        }
    }

    private class Popper implements Runnable {

        private int times;
        private ThreadSafeList<Integer> list;

        Popper(int times, ThreadSafeList<Integer> list){
            this.times = times;
            this.list = list;
        }

        @Override
        public void run(){
            //noinspection ListRemoveInLoop
            for(int i = 0; i < this.times; i++){
                this.list.remove(0);
            }
        }
    }
}
