package server;

import server.Exception.*;
import util.Pair;
import util.ThreadSafeMap;
import util.ThreadSafeMutMap;

import java.util.*;
import java.util.stream.Collectors;


public class AuctionHouse {
    private Map<String, User> users;
    private ThreadSafeMutMap<ServerType, Auction> auctions;
    private Map<Integer, Droplet> reservedD;
    private ThreadSafeMutMap<Integer, Auction> reservedA;
    private Map<ServerType, Integer> stock;

    AuctionHouse() {
        this.users = new ThreadSafeMap<>();
        this.auctions = new ThreadSafeMutMap<>();
        this.reservedD = new ThreadSafeMap<>();
        this.reservedA = new ThreadSafeMutMap<>();
        this.stock = new ThreadSafeMap<>();
    }

    public void signUp(String email, String password, String name) throws UserAlreadyExistsException {
        User u = this.users.get(email);
        if (u == null) {
            throw new UserAlreadyExistsException("Email already registered");
        }
        this.users.put(email, new User(name, email, password));
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

    public Pair<List<Auction>, List<Droplet>> listOwnedServers(String email) {
        List<Droplet> droplets = this.reservedD.values().stream()
                .filter(d -> email.equals(d.getOwner().getEmail()))
                .collect(Collectors.toList());

        List<Auction> auctions = new LinkedList<>();
        for (Auction a : this.reservedA.valuesLocked()) {
            if (email.equals(a.highestBidder().getEmail())) {
                auctions.add((Auction) a.clone());
            }
            a.unlock();
        }

        return Pair.of(auctions, droplets);
    }

    public int dropServer(int serverID, User user) throws ServerNotFound, ServerPermissionException {
        Auction a = this.reservedA.getLocked(serverID);
        if (a != null) {
            try {
                if (!(a.highestBidder().equals(user))) {
                    throw new ServerPermissionException("That server doesn't belong to you");
                } else {
                    this.reservedA.remove(serverID);
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
                this.reservedD.remove(serverID);
                return d.getId();
            }
        }

        throw new ServerNotFound("No server with that id found");
    }

    public List<ServerType> listAvailableServers() {
        return this.stock.entrySet().stream()
                .filter(p -> p.getKey().cost() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // TODO This should be a transaction
    public int requestDroplet(ServerType st, User user) throws DropletOfTypeWithoutStock {
        if (this.stock.get(st) == 0) {
            throw new DropletOfTypeWithoutStock("No stock for type: " + st.getName());
        } else {
            Droplet d = new Droplet(user, st);
            this.reservedD.put(d.getId(), d);

            Integer previousStock = this.stock.get(st);
            this.stock.put(st, previousStock - 1);

            return d.getId();
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

        Auction previous = this.auctions.getLocked(st);
        if (previous != null) {
            previous.unlock();
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
