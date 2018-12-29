package server;

import server.exception.*;
import util.Pair;
import util.ThreadSafeMap;
import util.ThreadSafeMutMap;

import java.util.*;
import java.util.stream.Collectors;


public class AuctionHouse {
    private Map<String, User> users;
    private ThreadSafeMap<String, Float> debtDeadServers;
    private ThreadSafeMutMap<ServerType, Auction> auctions;
    private Map<Integer, Droplet> reservedD;
    private ThreadSafeMutMap<Integer, Auction> reservedA;
    private ThreadSafeMap<ServerType, Integer> stock;

    private static final int initialStock = 20;

    AuctionHouse() {
        this.users = new ThreadSafeMap<>();
        this.debtDeadServers = new ThreadSafeMap<>();
        this.auctions = new ThreadSafeMutMap<>();
        this.reservedD = new ThreadSafeMap<>();
        this.reservedA = new ThreadSafeMutMap<>();
        this.stock = new ThreadSafeMap<>();

        Arrays.stream(ServerType.values()).forEach(st -> this.stock.put(st, AuctionHouse.initialStock));
    }

    public void signUp(String email, String password, String name) throws UserAlreadyExistsException {
        User u = this.users.get(email);
        if (u != null) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        this.users.put(email, new User(name, email, password));
        this.debtDeadServers.put(email, 0f);
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

    public List<Pair<ServerType, Integer>> listOwnedServers(String email) {
        return this.stock.entrySet().stream()
                .filter(p -> p.getValue() > 0)
                .map(p -> Pair.of(p.getKey(), p.getValue()))
                .collect(Collectors.toList());
    }

    public int dropServer(int serverID, User user) throws ServerNotFound, ServerPermissionException {
        Auction a = this.reservedA.getLocked(serverID);
        if (a != null) {
            try {
                if (!(a.highestBidder().equals(user))) {
                    throw new ServerPermissionException("That server doesn't belong to you");
                } else {
                    this.stock.lock();
                    this.debtDeadServers.lock();
                    this.reservedA.remove(serverID);

                    float newCost = this.debtDeadServers.get(user.getEmail()) + a.cost();
                    this.debtDeadServers.put(user.getEmail(), newCost);
                    int newStock = this.stock.get(a.getType()) + 1;
                    this.stock.put(a.getType(), newStock);

                    this.stock.unlock();
                    this.debtDeadServers.unlock();
                    return a.getId();
                }
            } finally {
                a.unlock();
            }
        }

        if (this.reservedD.containsKey(serverID)) {
            Droplet d = this.reservedD.remove(serverID);
            if (!(d.getOwner().equals(user))) {
                throw new ServerPermissionException("That server doesn't belong to you");
            } else {
                this.stock.lock();
                this.debtDeadServers.lock();
                this.reservedD.remove(serverID);

                float newCost = this.debtDeadServers.get(user.getEmail()) + d.cost();
                this.debtDeadServers.put(user.getEmail(), newCost);

                int newStock = this.stock.get(d.getItem()) + 1;
                this.stock.put(d.getItem(), newStock);

                this.debtDeadServers.lock();
                this.stock.unlock();
                return d.getId();
            }
        }

        throw new ServerNotFound("No server with that id found");
    }

    public Pair<List<ServerType>, List<Integer>> listAvailableServers() {
        List<ServerType> stL = new LinkedList<>();
        List<Integer> stockL = new LinkedList<>();

        this.stock.lock();
        for (Map.Entry<ServerType, Integer> kv : this.stock.entrySet()) {
            if (kv.getValue() > 0) {
                stL.add(kv.getKey());
                stockL.add(kv.getValue());
            }
        }
        this.stock.unlock();

        return Pair.of(stL, stockL);
    }

    public int requestDroplet(ServerType st, User user) throws DropletOfTypeWithoutStock {
        this.stock.lock();
        try {
            if (this.stock.get(st) == 0) {
                throw new DropletOfTypeWithoutStock("No stock for type: " + st.getName());
            } else {
                Droplet d = new Droplet(user, st);
                this.reservedD.put(d.getId(), d);

                Integer previousStock = this.stock.get(st);
                this.stock.put(st, previousStock - 1);

                return d.getId();
            }
        } finally {
            this.stock.unlock();
        }
    }

    public List<Auction> listRunningAuctions() {
        List<Auction> auctionsL = new LinkedList<>();
        for (Auction a : this.auctions.valuesLocked()) {
            auctionsL.add((Auction) a.clone());
            a.unlock();
        }
        return auctionsL;
    }

    public int startAuction(ServerType st, Bid bid) throws AuctionOfTypeRunningException {
        Objects.requireNonNull(st);
        Objects.requireNonNull(bid);

        if (this.auctions.containsKey(st)) {
            throw new AuctionOfTypeRunningException("An auction of that type is already running");
        } else {
            Auction auction = new Auction(st, bid);
            int auctionId = auction.getId();
            auctions.putLocked(st, auction);
            return auctionId;
        }
    }

    public void bid(int aucID, Bid bid) throws LowerBidException, InvalidAuctionException, NoRunningAuctionsException {
        Objects.requireNonNull(bid);

        Collection<Auction> aucs = this.auctions.valuesLocked();
        if (aucs.isEmpty()) {
            throw new NoRunningAuctionsException("No auctions running");
        } else {
            Auction auc = null;
            for (Auction a : aucs) {
                if (aucID == a.getId()) {
                    auc = a;
                } else {
                    a.unlock();
                }
            }
            if (auc == null) {
                throw new InvalidAuctionException("No auction with that id running");
            } else {
                try {
                    auc.bid(bid);
                } finally {
                    auc.unlock();
                }
            }
        }
    }
}
