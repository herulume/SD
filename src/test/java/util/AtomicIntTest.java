package util;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AtomicIntTest {

    @Test
    public void fetchOp() throws InterruptedException {
        final AtomicInt aInt = new AtomicInt(0);
        List<Thread> threads = IntStream.range(0, 1000)
                .mapToObj(i -> new Thread(() -> aInt.fetchAndApply(x -> x + 1)))
                .collect(Collectors.toList());
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            thread.join();
        }
        Assert.assertEquals(aInt.load(), 1000);
    }
}
