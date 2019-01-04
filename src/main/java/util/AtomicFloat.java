package util;

import java.util.function.Function;

public class AtomicFloat {

    private float value;

    public AtomicFloat(float value) {
        this.value = value;
    }

    public synchronized float load() {
        return this.value;
    }

    public synchronized void store(float value) {
        this.value = value;
    }

    public synchronized float swap(float value) {
        float old = this.value;
        this.value = value;
        return old;
    }

    public synchronized float fetchAndApply(Function<Float, Float> op) {
        float old = this.value;
        this.value = op.apply(this.value);
        return old;
    }
}
