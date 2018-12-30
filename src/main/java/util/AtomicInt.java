package util;

import java.util.function.Function;

public class AtomicInt {

    private int value;

    public AtomicInt(int value) {
        this.value = value;
    }

    public synchronized int load() {
        return this.value;
    }

    public synchronized void store(int value) {
        this.value = value;
    }

    public synchronized int swap(int value) {
        int old = this.value;
        this.value = value;
        return old;
    }

    public synchronized int fetchOp(Function<Integer,Integer> op) {
        int old = this.value;
        this.value = op.apply(this.value);
        return old;
    }

}
