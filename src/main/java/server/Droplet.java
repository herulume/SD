package server;

import util.AtomicInt;

import java.util.Objects;

public class Droplet {

    private final int id;
    private final User owner;
    private final ServerType serverType;
    private final float cost;

    private static AtomicInt idN = new AtomicInt(0);

    Droplet(User owner, ServerType serverType) {
        this.id = Droplet.idN.fetchAndApply(x -> x + 1);
        this.owner = Objects.requireNonNull(owner);
        this.serverType = Objects.requireNonNull(serverType);
        this.cost = serverType.getPrice();
    }

    Droplet(User owner, ServerType serverType, float cost) {
        this.id = Droplet.idN.fetchAndApply(x -> x + 1);
        this.owner = Objects.requireNonNull(owner);
        this.serverType = Objects.requireNonNull(serverType);
        this.cost = cost;
    }

    int getId() {
        return this.id;
    }

    User getOwner() {
        return this.owner;
    }

    ServerType getServerType() {
        return this.serverType;
    }

    float getCost() {
        return this.cost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Droplet droplet = (Droplet) o;
        return id == droplet.id;
    }

    @Override
    public String toString() {
        return this.id + "\t" + this.serverType.getName() + "\t" + this.cost;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
