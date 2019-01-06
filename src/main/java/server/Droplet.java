package server;

import util.AtomicInt;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class Droplet {

    private final int id;
    private final User owner;
    private final ServerType serverType;
    private final float cost;
    private final LocalDateTime timestamp;

    private static final AtomicInt idN = new AtomicInt(0);

    Droplet(User owner, ServerType serverType) {
        this.id = Droplet.idN.fetchAndApply(x -> x + 1);
        this.owner = Objects.requireNonNull(owner);
        this.serverType = Objects.requireNonNull(serverType);
        this.cost = serverType.getPrice();
        this.timestamp = LocalDateTime.now();
    }

    Droplet(User owner, ServerType serverType, float cost) {
        this.id = Droplet.idN.fetchAndApply(x -> x + 1);
        this.owner = Objects.requireNonNull(owner);
        this.serverType = Objects.requireNonNull(serverType);
        this.cost = cost;
        this.timestamp = LocalDateTime.now();
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

    float getDebt() {
        return this.cost * (this.timestamp.until(LocalDateTime.now(), ChronoUnit.SECONDS) / (3600.0f));
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
        return String.format("%-3d %-10s %16.2f", this.id, this.serverType.getName(), this.getDebt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
