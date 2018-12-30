package server;

import server.exception.*;
import util.Pair;
import util.ThreadSafeMap;
import util.ThreadSafeMutMap;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class AuctionHouse {

    private ThreadSafeMap<String, User> users;
    private ThreadSafeMap<String, Float> debtDeadServers;
    private ThreadSafeMutMap<ServerType, Auction> auctions;
    private ThreadSafeMap<Integer, Droplet> reservedD;
    private ThreadSafeMap<Integer, Droplet> reservedA;
    private ThreadSafeMap<ServerType, Integer> stock;

    private static final int initialStock = 20;

    AuctionHouse() {
        this.users = new ThreadSafeMap<>();
        this.debtDeadServers = new ThreadSafeMap<>();
        this.auctions = new ThreadSafeMutMap<>();
        this.reservedD = new ThreadSafeMap<>();
        this.reservedA = new ThreadSafeMap<>();
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
        ThreadSafeMap<Integer, Droplet> target;
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
        Droplet toRemove = target.get(serverID);
        if (toRemove.getOwner().equals(user)) {
            this.stock.lock();
            this.debtDeadServers.lock();
            target.remove(serverID);

            float newCost = this.debtDeadServers.get(user.getEmail()) + toRemove.getCost();
            this.debtDeadServers.put(user.getEmail(), newCost);
            int newStock = this.stock.get(toRemove.getServerType()) + 1;
            this.stock.put(toRemove.getServerType(), newStock);

            this.stock.unlock();
            this.debtDeadServers.unlock();
            target.unlock();
            return toRemove.getId();
        } else {
            target.unlock();
            throw new ServerPermissionException("That server doesn't belong to you");
        }
    }

    public List<Pair<ServerType, Integer>> listAvailableServers() {
        return this.stock.entrySet().stream()
                .filter(p -> p.getValue() > 0)
                .map(p -> Pair.of(p.getKey(), p.getValue()))
                .collect(Collectors.toList());
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

    public List<Auction.AuctionView> listRunningAuctions() {
        List<Auction.AuctionView> auctionsL = new LinkedList<>();
        for (Auction a : this.auctions.valuesLocked()) {
            auctionsL.add(a.getView());
            a.unlock();
        }
        return auctionsL;
    }

    public void auction(ServerType st, Bid bid) {
        this.auctions.lock();
        Auction auction = this.auctions.getLocked(st);
        if (auction == null) {
            auction = new Auction(st, bid, this::reserveAuctioned);
            Auction shouldBeNull = auctions.putLocked(st, auction);
            this.auctions.unlock();
            assert shouldBeNull == null;
        } else {
            this.auctions.unlock();
            auction.bid(bid);
            auction.unlock();
        }
    }

    private Optional<Integer> reserveAuctioned(ServerType st, Bid bid) {
        this.auctions.remove(st);
        this.stock.lock();
        try {
            if (this.stock.get(st) == 0) {
                return Optional.empty();
            } else {
                Droplet d = new Droplet(bid.getBidder(), st, bid.getValue());
                this.reservedA.put(d.getId(), d);
                this.stock.put(st, this.stock.get(st) - 1);
                return Optional.of(d.getId());
            }
        } finally {
            this.stock.unlock();
        }
    }
}
