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

    public synchronized int fetchAndApply(Function<Integer, Integer> op) {
        int old = this.value;
        this.value = op.apply(this.value);
        return old;
    }

    public synchronized void apply(Function<Integer, Integer> op) {
        this.value = op.apply(this.value);
    }

}
