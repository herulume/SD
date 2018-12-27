package server;

import java.util.Objects;

public class Droplet {
    private final int id;
    private User owner;
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

    String getOwner() {
        return this.owner.getName();
    }

    ServerType getItem() {
        return this.item;
    }
}
