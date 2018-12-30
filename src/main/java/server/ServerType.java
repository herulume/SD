package server;

import java.util.Optional;

public enum ServerType {
    I(12.3f, "ts1.lower"), B(203.3f, "ts2.high");

    private final float cost;
    private final String name;

    ServerType(float cost, String name) {
        this.cost = cost;
        this.name = name;
    }

    public float cost() {
        return this.cost;
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
}
