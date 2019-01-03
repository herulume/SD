package server;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class AQueue {

    private final PriorityQueue<Bid> bids;
    private final Consumer<Bid> popCallback;

    AQueue(Consumer<Bid> popCallback) {
        this.bids = new PriorityQueue<>(Comparator.reverseOrder());
        this.popCallback = popCallback;
    }

    // pushing a bid from the same user will replace any previous bids, regardless of value.
    synchronized void enqueue(Bid bid) {
        this.bids.removeIf(x -> x.getBidder().equals(bid.getBidder()));
        this.bids.add(bid);
    }

    synchronized void serve() {
        if (this.bids.size() > 0)
            popCallback.accept(this.bids.poll());
    }
}
