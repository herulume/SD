package server;

import server.exception.LowerBidException;
import util.Lockable;

import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Auction implements Lockable {
    private final int id;
    private final ServerType item;
    private Bid highestBid;

    private ReadWriteLock lock;

    private static int idN = 0;

    public Auction(ServerType st, Bid bid) {
        this.item = Objects.requireNonNull(st);
        this.id = idN;
        this.highestBid = Objects.requireNonNull(bid);
        this.lock = new ReentrantReadWriteLock();

        Auction.idN++;
    }

    public void bid(Bid bid) throws LowerBidException {
        try {
            this.lock.writeLock().lock();
            Objects.requireNonNull(bid);
            if (bid.value() <= this.highestBid.value())
                throw new LowerBidException("Bid value lower than current bid");
            this.highestBid = bid;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public int getId() {
        return this.id;
    }

    public ServerType getType() {
        return this.item;
    }

    public float highestBid() {
        try {
            this.lock.readLock().lock();
            return this.highestBid.value();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public User highestBidder() {
        try {
            this.lock.readLock().lock();
            return this.highestBid.buyer();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    // TODO Implement this after putting the timer
    public float cost() {
        return this.highestBid();
    }

    @Override
    public void lock() {
        this.lock.writeLock().lock();
    }

    @Override
    public void unlock() {
        this.lock.writeLock().unlock();
    }

    public Object clone() {
        try {
            this.lock.readLock().lock();
            Auction copy = new Auction(this.id, this.item);
            copy.highestBid = this.highestBid;
            return copy;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private Auction(int id, ServerType st) {
        this.item = st;
        this.id = id;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Auction auction = (Auction) o;
        return id == auction.id;
    }

    @Override
    public String toString(){
        return this.id + "\t" + this.item.getName() + "\t" + highestBid.value();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
