package server;

import server.middleware.Session;
import util.Lockable;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

public class Auction implements Lockable {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final ServerType serverType;
    private final PriorityQueue<Bid> bids;
    private final ScheduledFuture<?> callback;

    private ReadWriteLock lock;


    public Auction(ServerType st, Bid bid, BiFunction<ServerType,Bid,Optional<Integer>> endCallback) {
        this.serverType = Objects.requireNonNull(st);

        this.bids = new PriorityQueue<>(Comparator.reverseOrder());
        this.bids.add(Objects.requireNonNull(bid));

        this.lock = new ReentrantReadWriteLock();

        this.callback = scheduler.schedule(() -> {
            Optional<Integer> id = endCallback.apply(this.serverType, this.highestBid());
            this.lock.writeLock().lock();
            if (id.isPresent())
                Objects.requireNonNull(this.bids.poll()).getSession().notifyWon(this.serverType, id.get());
            else
                Objects.requireNonNull(this.bids.poll()).getSession().notifyOutOfStock(this.serverType);
            while (!this.bids.isEmpty()) {
                this.bids.poll().getSession().notifyLost(this.serverType);
            }
            this.lock.writeLock().unlock();
        }, 10, TimeUnit.SECONDS);
    }

    void bid(Bid bid) {
        this.lock.writeLock().lock();
        this.bids.add(bid);
        this.lock.writeLock().unlock();
    }

    private Bid highestBid() {
        this.lock.writeLock().lock();
        try {
            assert this.bids.peek() != null;
            return this.bids.peek();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    boolean hasSession(Session s) {
        try {
            this.lock.readLock().lock();
            return this.bids.stream().anyMatch(b -> b.getSession() == s /*using == on purpose*/);
        } finally {
            this.lock.readLock().unlock();
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

    public AuctionView getView() {
        this.lock.readLock().lock();
        try {
            assert this.bids.peek() != null;
            return new AuctionView(this.bids.peek().getValue(), this.callback.getDelay(TimeUnit.SECONDS), this.serverType);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public class AuctionView {

        private final float highestBid;
        private final long timeLeft;
        private final ServerType serverType;

        private AuctionView(float highestBid, long timeLeft, ServerType serverType) {
            this.highestBid = highestBid;
            this.timeLeft = timeLeft;
            this.serverType = serverType;
        }

        @Override
        public String toString() {
            return this.serverType + "\t" + this.highestBid + "\t" + this.timeLeft;
        }
    }
}
