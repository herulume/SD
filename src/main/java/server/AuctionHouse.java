package server;

import server.Exception.*;
import util.ThreadSafeMap;
import util.ThreadSafeMutMap;

import java.util.*;


public class AuctionHouse {
    private Map<String, User> users;
    private ThreadSafeMutMap<ServerType, Auction> auctions;
    private Map<Integer, Droplet> reservedD;
    private Map<Integer, Auction> reservedA;
    private Map<ServerType, Integer> stock;

    AuctionHouse() {
        this.users = new ThreadSafeMap<>();
        this.auctions = new ThreadSafeMutMap<>();
        this.reservedD = new ThreadSafeMap<>();
        this.reservedA = new ThreadSafeMap<>();
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

    public List<Auction> listRunningAuctions() {
        List<Auction> auctionsL = new LinkedList<>();
        this.auctions.valuesLocked().forEach(a -> {
            auctionsL.add((Auction) a.clone());
            a.unlock();
        });
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

    public void bid(int auctionID, Bid bid) throws LowerBidException, InvalidAuctionException, NoRunningAuctionsException {
        Objects.requireNonNull(bid);

        Collection<Auction> aucs = this.auctions.valuesLocked();
        if (aucs.isEmpty()) {
            throw new NoRunningAuctionsException("No auctions running");
        } else {
            Auction auc = null;
            for (Auction a : aucs) {
                if (auctionID == a.getId()) {
                    auc = a;
                } else {
                    a.unlock();
                }
            }
            if (auc == null) {
                throw new InvalidAuctionException("No auction with that id running");
            } else {
                auc.bid(bid);
            }
        }
    }
}
