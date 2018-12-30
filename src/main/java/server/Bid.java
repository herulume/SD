package server;

import server.middleware.Session;

public class Bid implements Comparable<Bid> {

    private final Session session;
    private final float value;
    private final User bidder;

    public Bid(Session session, float value, User bidder) {
        this.session = session;
        this.value = value;
        this.bidder = bidder;
    }

    float getValue() {
        return this.value;
    }

    Session getSession() {
        return this.session;
    }

    User getBidder() {
        return this.bidder;
    }

    @Override
    public int compareTo(Bid bid) {
        return Float.compare(this.value, bid.value);
    }
}
