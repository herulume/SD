package server;

public class Auction {
    private final int id;
    private final ServerType item;
    private Bid highestBid;

    private static int idN = 0;

    public Auction(ServerType st) {
        this.item = st;
        this.id = idN;

        Auction.idN++;
    }

    public int getId() {
        return this.id;
    }

    public ServerType getType() {
        return this.item;
    }

    public float actualCost() {
        return this.item.cost();
    }

    public float highestBid() {
        return this.highestBid.value();
    }

    private String highestBidder() {
        return this.highestBid.buyer();
    }
}
