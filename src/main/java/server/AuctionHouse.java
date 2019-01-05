package server;

import server.exception.*;
import util.*;

import java.util.*;
import java.util.stream.Collectors;


public class AuctionHouse {

    private ThreadSafeMap<String, User> users;
    private ThreadSafeMap<String, AtomicFloat> debtDeadServers;
    private ThreadSafeMutMap<ServerType, Auction> auctions;
    private ThreadSafeMap<ServerType, UniqueBidQueue> queues;
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
            this.queues.put(st, new UniqueBidQueue(bid -> this.reserveQueued(st, bid)));
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
        Objects.requireNonNull(user);
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
            ServerType st;
            this.stock.lock();
            this.debtDeadServers.lock();
            try {
                st = target.remove(serverID).getServerType();
                this.debtDeadServers.get(user.getEmail()).apply(x -> x + toRemove.getDebt());
                this.stock.get(toRemove.getServerType()).apply(x -> x + 1);
                this.queues.get(st).serve();
            } finally {
                this.debtDeadServers.unlock();
                this.stock.unlock();
                target.unlock();
            }
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
                this.reservedA.lock();
                try {
                    Droplet toSteal = this.reservedA.values().stream()
                            .filter(d -> d.getServerType() == st && !d.getOwner().equals(user))
                            .findAny()
                            .orElseThrow(() -> new DropletOfTypeWithoutStock("No stock for type: " + st.getName()));
                    this.reservedA.remove(toSteal.getId());
                    this.debtDeadServers.get(toSteal.getOwner().getEmail()).apply(x -> x + toSteal.getDebt());
                    toSteal.getOwner().sendNotification("Your auctioned droplet with id " + toSteal.getId() + "was sold to another user!");
                } finally {
                    this.reservedA.unlock();
                }
                Droplet stolen = new Droplet(user, st);
                this.reservedD.put(stolen.getId(), stolen);
                return stolen.getId();
            } else {
                Droplet d = new Droplet(user, st);
                this.reservedD.put(d.getId(), d);
                this.stock.get(st).apply(x -> x - 1);
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

    public enum AuctionKind { TIMED_STARTED, TIMED_REBIDED, QUEUED }

    public AuctionKind auction(ServerType st, Bid bid) throws BidTooLowException {
        Objects.requireNonNull(st);
        Objects.requireNonNull(bid);
        this.auctions.lock();
        try{
            if (this.auctions.containsKey(st)) {
                Auction auction = this.auctions.getLocked(st);
                try{
                    auction.bid(bid);
                    return AuctionKind.TIMED_REBIDED;
                } finally {
                    auction.unlock();
                }
            } else {
                this.stock.lock();
                try {
                    if (this.stock.get(st).load() == 0) {
                        this.queues.get(st).enqueue(bid);
                        return AuctionKind.QUEUED;
                    } else {
                        Auction a = new Auction(st, bid, b -> reserveAuctioned(st, b));
                        this.stock.get(st).apply(x -> x - 1);
                        auctions.putLocked(st, a);
                        return AuctionKind.TIMED_STARTED;
                    }
                } finally {
                    this.stock.unlock();
                }
            }
        } finally {
            this.auctions.unlock();
        }
    }

    private int reserveAuctioned(ServerType st, Bid bid) {
        this.auctions.remove(st);
        Droplet d = new Droplet(bid.getBidder(), st, bid.getValue());
        this.reservedA.put(d.getId(), d);
        return d.getId();
    }

    private void reserveQueued(ServerType st, Bid bid) {
        this.stock.lock();
        try {
            if (this.stock.get(st).load() > 0) {
                this.stock.get(st).apply(x -> x - 1);
                Droplet d = new Droplet(bid.getBidder(), st, bid.getValue());
                this.reservedA.put(d.getId(), d);
                bid.getBidder().sendNotification("Server: " + st + " now in stock! Reserved with id: " + d.getId());
            } else {
                this.queues.get(st).enqueue(bid);
            }
        } finally {
            this.stock.unlock();
        }
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
