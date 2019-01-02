package server;

import server.exception.*;
import util.*;

import java.util.*;
import java.util.stream.Collectors;


public class AuctionHouse {

    private ThreadSafeMap<String, User> users;
    private ThreadSafeMap<String, AtomicFloat> debtDeadServers;
    private ThreadSafeMutMap<ServerType, Auction> auctions;
    private ThreadSafeMap<ServerType, AQueue> queues;
    private ThreadSafeMap<Integer, Droplet> reservedD;
    private ThreadSafeMap<Integer, Droplet> reservedA;
    private ThreadSafeMap<ServerType, AtomicInt> stock;

    private static final int initialStock = 4;

    AuctionHouse() {
        this.users = new ThreadSafeMap<>();
        this.debtDeadServers = new ThreadSafeMap<>();
        this.auctions = new ThreadSafeMutMap<>();
        this.queues = new ThreadSafeMap<>();
        this.reservedD = new ThreadSafeMap<>();
        this.reservedA = new ThreadSafeMap<>();
        this.stock = new ThreadSafeMap<>();

        Arrays.stream(ServerType.values()).forEach(st -> {
            this.stock.put(st, new AtomicInt(AuctionHouse.initialStock));
            this.queues.put(st, new AQueue(st, this::reserveQueued));
        });
    }

    public void signUp(String email, String password, String name) throws UserAlreadyExistsException {
        User u = this.users.get(email);
        if (u != null) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        this.users.put(email, new User(name, email, password));
        this.debtDeadServers.put(email, new AtomicFloat(0));
    }

    public User login(String email, String password) throws LoginException {
        User user = this.users.get(email);
        if (user == null || !user.authenticate(password)) {
            throw new LoginException("Your email or username were incorrect");
        } else {
            user.reset();
        }
        return user;
    }

    public Pair<List<Droplet>, List<Droplet>> listOwnedServers(String email) {
        return Pair.of(
                this.reservedD.values().stream()
                        .filter(d -> email.equals(d.getOwner().getEmail()))
                        .collect(Collectors.toList()),
                this.reservedA.values().stream()
                        .filter(d -> email.equals(d.getOwner().getEmail()))
                        .collect(Collectors.toList())
        );
    }

    public int dropServer(int serverID, User user) throws ServerNotFoundException, ServerPermissionException {
        this.reservedD.lock();
        this.reservedA.lock();
        final ThreadSafeMap<Integer, Droplet> target;
        if (this.reservedD.containsKey(serverID)) {
            this.reservedA.unlock();
            target = this.reservedD;
        } else if (this.reservedA.containsKey(serverID)) {
            this.reservedD.unlock();
            target = this.reservedA;
        } else {
            this.reservedD.unlock();
            this.reservedA.unlock();
            throw new ServerNotFoundException("No server with that id found");
        }
        final Droplet toRemove = target.get(serverID);
        if (toRemove.getOwner().equals(user)) {
            this.stock.lock();
            this.debtDeadServers.lock();
            ServerType st = target.remove(serverID).getServerType();

            this.debtDeadServers.get(user.getEmail()).fetchAndApply(x -> x + toRemove.getDebt());
            this.stock.get(toRemove.getServerType()).fetchAndApply(x -> x + 1);

            this.stock.unlock();
            this.debtDeadServers.unlock();
            target.unlock();
            this.queues.get(st).pop();
            return toRemove.getId();
        } else {
            target.unlock();
            throw new ServerPermissionException("That server doesn't belong to you");
        }
    }

    public List<Pair<ServerType, Integer>> listAvailableServers() {
        return this.stock.entrySet().stream()
                .map(p -> Pair.of(p.getKey(), p.getValue().load()))
                .collect(Collectors.toList());
    }

    public int requestDroplet(ServerType st, User user) throws DropletOfTypeWithoutStock {
        this.stock.lock();
        try {
            if (this.stock.get(st).load() == 0) {
                throw new DropletOfTypeWithoutStock("No stock for type: " + st.getName());
            } else {
                Droplet d = new Droplet(user, st);
                this.reservedD.put(d.getId(), d);
                this.stock.get(st).fetchAndApply(x -> x - 1);
                return d.getId();
            }
        } finally {
            this.stock.unlock();
        }
    }

    public List<Auction.View> listRunningAuctions() {
        List<Auction.View> auctionsL = new LinkedList<>();
        for (Auction a : this.auctions.valuesLocked()) {
            auctionsL.add(a.getView());
            a.unlock();
        }
        return auctionsL;
    }

    public enum AuctionKind { TIMED, QUEUED }

    public AuctionKind auction(ServerType st, Bid bid) throws BidTooLowException {
        Objects.requireNonNull(st);
        Objects.requireNonNull(bid);
        this.stock.lock();
        if (this.stock.get(st).load() == 0) {
            this.queues.get(st).push(bid);
            return AuctionKind.QUEUED;
        } else {
            this.auctions.lock();
            this.stock.get(st).fetchAndApply(x -> x - 1);
            this.stock.unlock();
            Auction auction = this.auctions.getLocked(st);
            try {
                if (auction == null) {
                    Auction a = new Auction(st, bid, this::reserveAuctioned);
                    Auction shouldBeNull = auctions.putLocked(st, a);
                    assert shouldBeNull == null;
                } else {
                    auction.bid(bid);
                }
            } finally {
                this.auctions.unlock();
                if (auction != null) auction.unlock();
            }
            return AuctionKind.TIMED;
        }
    }

    private int reserveAuctioned(ServerType st, Bid bid) {
        this.auctions.remove(st);
        Droplet d = new Droplet(bid.getBidder(), st, bid.getValue());
        this.reservedA.put(d.getId(), d);
        bid.getBidder().sendNotification("Server: " + st + " reserved with id: " + d.getId());
        return d.getId();
    }

    private void reserveQueued(ServerType st, Bid bid) {
        this.stock.lock();
        if (this.stock.get(st).load() > 0) {
            this.stock.get(st).fetchAndApply(x -> x - 1);
            Droplet d = new Droplet(bid.getBidder(), st, bid.getValue());
            this.reservedA.put(d.getId(), d);
            bid.getBidder().sendNotification("Server: " + st + " now in stock! Reserved with id: " + d.getId());
        } else {
            this.queues.get(st).push(bid);
        }
        this.stock.unlock();
    }

    public Pair<Float, Float> getDebt(User user) {
        Objects.requireNonNull(user);
        return Pair.of(
                this.debtDeadServers.get(user.getEmail()).load(),
                (float) (this.reservedD.values().stream()
                        .filter(d -> d.getOwner().equals(user))
                        .mapToDouble(Droplet::getDebt)
                        .sum()
                        + this.reservedA.values().stream()
                        .filter(d -> d.getOwner().equals(user))
                        .mapToDouble(Droplet::getDebt)
                        .sum())
        );
    }

}
