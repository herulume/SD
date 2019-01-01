package server;

import server.exception.BidTooLowException;
import util.Lockable;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class Auction implements Lockable {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ServerType serverType;
    private final PriorityQueue<Bid> bids;
    private final ScheduledFuture<?> callback;
    private final ReadWriteLock lock;


    public Auction(ServerType st, Bid bid, Function<Bid, Integer> endCallback) {
        Objects.requireNonNull(endCallback);
        this.serverType = Objects.requireNonNull(st);

        this.bids = new PriorityQueue<>(Comparator.reverseOrder());
        this.bids.add(Objects.requireNonNull(bid));

        this.lock = new ReentrantReadWriteLock();

        this.callback = scheduler.schedule(() -> {
            this.lock.writeLock().lock();
            int id = endCallback.apply(this.highestBid());
            Objects.requireNonNull(this.bids.poll()).getBidder().sendNotification("Won " + this.serverType + " with id: " + id);
            while (!this.bids.isEmpty()) {
                this.bids.poll().getBidder().sendNotification("Lost auction: " + this.serverType);
            }
            this.lock.writeLock().unlock();
        }, 40, TimeUnit.SECONDS);
    }

    void bid(Bid bid) throws BidTooLowException {
        Objects.requireNonNull(bid);
        this.lock.writeLock().lock();
        try {
            if (this.highestBid().getValue() >= bid.getValue())
                throw new BidTooLowException("Please bid higher then the current highest bid: " + highestBid().getValue());
            this.bids.removeIf(x -> x.getBidder().equals(bid.getBidder()));
            this.bids.add(bid);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private Bid highestBid() {
        this.lock.writeLock().lock();
        try {
            return Objects.requireNonNull(this.bids.peek());
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public void lock() {
        this.lock.writeLock().lock();
    }

    @Override
    public void unlock() {
        this.lock.writeLock().unlock();
    }

    View getView() {
        this.lock.readLock().lock();
        try {
            assert this.bids.peek() != null;
            return new View(this.bids.peek().getValue(), this.callback.getDelay(TimeUnit.SECONDS), this.serverType);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public class View {

        private final float highestBid;
        private final long timeLeft;
        private final ServerType serverType;

        private View(float highestBid, long timeLeft, ServerType serverType) {
            this.highestBid = highestBid;
            this.timeLeft = timeLeft;
            this.serverType = serverType;
        }

        @Override
        public String toString() {
            return this.serverType + "\t" + this.highestBid + "\t\t" + this.timeLeft;
        }
    }
}
