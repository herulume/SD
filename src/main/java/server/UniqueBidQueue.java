package server;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Consumer;

public class UniqueBidQueue {

    private final PriorityQueue<Bid> bids;
    private final Consumer<Bid> popCallback;
    private ServerType serverType;

    UniqueBidQueue(ServerType serverType, Consumer<Bid> popCallback) {
        Objects.requireNonNull(serverType);
        Objects.requireNonNull(popCallback);
        this.serverType = serverType;
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

    synchronized Optional<View> getView(User user){
        if(this.bids.size() < 1) return Optional.empty();
        Bid topBid = this.bids.peek();
        return Optional.of(new View(topBid.getValue(), topBid.getBidder().equals(user), this.bids.size(), this.serverType));
    }

    public class View {
        private float highestBid;
        private boolean isYours;
        private int size;
        private ServerType serverType;

        View(float highestBid, boolean isYours, int size, ServerType serverType) {
            this.highestBid = highestBid;
            this.isYours = isYours;
            this.size = size;
            this.serverType = serverType;
        }

        @Override
        public String toString() {
            return String.format("%-9s %5d %13.2f   %s",
                    this.serverType,
                    this.size,
                    this.highestBid,
                    (this.isYours ? "X" : " "));
        }
    }
}
