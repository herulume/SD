package server;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;

class AQueue {

    private final PriorityQueue<Bid> bids;
    private final BiConsumer<ServerType, Bid> popCallback;
    private ServerType serverType;

    AQueue(ServerType serverType, BiConsumer<ServerType, Bid> popCallback) {
        this.serverType = serverType;
        this.bids = new PriorityQueue<>(Comparator.reverseOrder());
        this.popCallback = popCallback;
    }

    synchronized void push(Bid bid) {
        this.bids.removeIf(x -> x.getBidder().equals(bid.getBidder()));
        this.bids.add(bid);
    }

    synchronized void pop() {
        if (this.bids.size() > 0)
            popCallback.accept(this.serverType, this.bids.poll());
    }
}
