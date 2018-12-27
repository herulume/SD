package server;

import java.util.Objects;

public class Bid {
    private final User buyer;
    private final float value;

    Bid(User buyer, float value) {
        this.buyer = Objects.requireNonNull(buyer);
        this.value = value;
    }

    String buyer() {
        return this.buyer.getName();
    }

    float value() {
        return this.value;
    }
}
