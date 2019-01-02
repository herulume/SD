package server;

public class Bid implements Comparable<Bid> {

    private final float value;
    private final User bidder;

    public Bid(float value, User bidder) {
        this.value = value;
        this.bidder = bidder;
    }

    float getValue() {
        return this.value;
    }

    User getBidder() {
        return this.bidder;
    }

    @Override
    public int compareTo(Bid bid) {
        return Float.compare(this.value, bid.value);
    }
}
