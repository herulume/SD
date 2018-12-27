package server;

public enum ServerType {
    I(12.3f, "SUPER BARATO"), B(203.3f, "SUPER CARO");

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
}
