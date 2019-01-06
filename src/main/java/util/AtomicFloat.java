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

    public synchronized void apply(Function<Float, Float> op) {
        this.value = op.apply(this.value);
    }
}
