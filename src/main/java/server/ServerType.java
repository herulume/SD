package server;

import java.util.Optional;

public enum ServerType {
    I(12.3f, "ts1.lower"), B(203.3f, "ts2.high");

    private final float price;
    private final String name;

    ServerType(float price, String name) {
        this.price = price;
        this.name = name;
    }

    public float getPrice() {
        return this.price;
    }

    public String getName() {
        return this.name;
    }

    public static Optional<ServerType> fromString(String st) {
        switch (st.toLowerCase()) {
            case "ts1.lower":
                return Optional.of(ServerType.I);
            case "ts2.high":
                return Optional.of(ServerType.B);
            default:
                return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
