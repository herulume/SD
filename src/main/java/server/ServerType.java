package server;

import java.util.Optional;

public enum ServerType {
    N(12.3f, "ts1.lower"),
    O(203.3f, "ts2.high"),
    C(107.8f, "ts3.mid"),
    A(600.9f, "ts4.sd");

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
                return Optional.of(ServerType.N);
            case "ts2.high":
                return Optional.of(ServerType.O);
            case "ts3.mid":
                return Optional.of(ServerType.C);
            case "ts4.sd":
                return Optional.of(ServerType.A);
            default:
                return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return this.name;
    }
}
