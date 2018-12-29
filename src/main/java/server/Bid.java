package server;

import java.util.Objects;

public class Bid {
    private final User buyer;
    private final float value;

    public Bid(User buyer, float value) {
        this.buyer = Objects.requireNonNull(buyer);
        this.value = value;
    }

    User buyer() {
        return this.buyer;
    }

    float value() {
        return this.value;
    }
}
