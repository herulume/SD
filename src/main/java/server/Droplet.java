package server;

import java.util.Objects;

public class Droplet {
    private final int id;
    private final User owner;
    private final ServerType item;

    private static int idN = 0;

    Droplet(User owner, ServerType item) {
        this.id = idN;
        this.owner = Objects.requireNonNull(owner);
        this.item = Objects.requireNonNull(item);

        Droplet.idN++;
    }

    int getId() {
        return this.id;
    }

    User getOwner() {
        return this.owner;
    }

    ServerType getItem() {
        return this.item;
    }

    // TODO Implement this after putting the timer
    float cost() {
        return this.item.cost();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Droplet droplet = (Droplet) o;
        return id == droplet.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
