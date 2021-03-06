package server.middleware;

import server.*;
import server.exception.*;
import util.Pair;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Session implements Runnable {

    // Common error messages
    private static final String LOGIN_REQUIRED = "You must be logged in to use this command";

    private User user;
    private final Socket socket;
    private PrintWriter out;
    private Thread ctt;
    private final AuctionHouse auctionHouse;

    public Session(Socket socket, AuctionHouse auctionHouse) {
        this.socket = socket;
        this.auctionHouse = auctionHouse;
        this.user = null;
        this.out = null;
        this.ctt = null;
    }

    @Override
    public void run() {
        BufferedReader in = null;
        try {
            try {
                this.out = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()),/*auto flush*/true);
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            } catch (IOException e) {
                System.out.println("Couldn't set up IO: " + e.getMessage());
                return;
            }
            String input;
            while ((input = in.readLine()) != null) {
                List<String> command = Arrays.stream(input.split(" "))
                        .filter(s -> s.length() != 0)
                        .collect(Collectors.toList());
                if (command.size() == 0) {
                    out.println();
                    continue;
                }
                if (command.get(0).equals("quit")) break;
                String response = runCommand(command);
                out.println(response);
            }
        } catch (IOException e) {
            System.out.println("Connection closed by client (User: " + this.user + "): " + e.getMessage());
        } finally {
            if (out != null) {
                out.close();
            }
            if (this.ctt != null) {
                this.ctt.interrupt();
            }
            try {
                if (in != null) {
                    in.close();
                }
                this.socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String runCommand(List<String> command) {
        switch (command.get(0)) {
            case "register": return register(command.subList(1, command.size()));
            case "login": return login(command.subList(1, command.size()));
            case "ls": return ls(command.subList(1, command.size()));
            case "buy": return buy(command.subList(1, command.size()));
            case "profile": return profile();
            case "drop": return dropServer(command.subList(1, command.size()));
            case "auction": return auction(command.subList(1, command.size()));
            case "help": return help();
            default: return "Command not found: " + command.get(0);
        }
    }

    private String help() {
        return "Available commands:\n"
                + "register      Register a new account\n"
                + "login         Log in to an existing account\n"
                + "ls            List Droplet state\n"
                + "buy           Purchase a droplets\n"
                + "auction       Auction a droplet\n"
                + "drop          Drop a droplet\n"
                + "profile       Consult you debt\n"
                + "help          Displays this message";
    }

    private String register(List<String> command) {
        if (command.size() < 3) return "Usage: register <email> <password> <username>";
        try {
            this.auctionHouse.signUp(command.get(0), command.get(1), command.get(2));
            return "Registered successfully";
        } catch (UserAlreadyExistsException e) {
            return e.getMessage();
        }
    }

    private String login(List<String> command) {
        if (this.user != null) return "You are already logged in!";
        if (command.size() < 2) return "Usage: login <email> <password>";
        try {
            this.user = this.auctionHouse.login(command.get(0), command.get(1));
            this.ctt = new Thread(new Ctt(this.user, this.out));
            this.ctt.start();
            return "Logged in";
        } catch (LoginException e) {
            return e.getMessage();
        }
    }

    private String ls(List<String> command) {
        if (this.user == null) return Session.LOGIN_REQUIRED;
        if (command.size() == 0) {
            return "NAME         PRICE  AMOUNT IN STOCK\n" +
                    this.auctionHouse.listAvailableServers()
                            .stream()
                            .map(x -> String.format("%-10s  %6.2f  %15d", x.getFirst().getName(), x.getFirst().getPrice(), x.getSecond()))
                            .sorted()
                            .reduce("", (x, y) -> x + "\n" + y);
        }
        if (command.get(0).equals("-m")) {
            return ls_m();
        }
        if (command.get(0).equals("-a")) {
            return ls_a();
        }
        if (command.get(0).equals("-q")) {
            return ls_q();
        }
        return "Usage: ls [OPTION]\n\t-m show my droplets\n\t-a show available auctions\n\t-q show running queues";
    }

    private String ls_m() {
        final String dropletHeader = "\nID  NAME       DEBT OWED SO FAR\n";
        Function<List<Droplet>, String> stringify = x -> x.stream().map(Droplet::toString).sorted().collect(Collectors.joining("\n"));
        Pair<String, String> possessions = this.auctionHouse.listOwnedServers(this.user.getEmail())
                .mapFirst(stringify)
                .mapSecond(stringify);
        StringBuilder result = new StringBuilder("DROPLETS:");
        if (possessions.getFirst().isEmpty()) {
            result.append(" You have no droplets\n");
        } else {
            result.append(dropletHeader).append(possessions.getFirst()).append("\n");
        }
        result.append("AUCTIONED DROPLETS:");
        if (possessions.getSecond().isEmpty()) {
            result.append(" You have no auctioned droplets\n");
        } else {
            result.append(dropletHeader).append(possessions.getSecond()).append("\n");
        }
        return result.toString();
    }

    private String ls_a() {
        String auctions = this.auctionHouse.listRunningAuctions()
                .stream()
                .map(Auction.View::toString)
                .reduce("", (x, y) -> x + "\n" + y);
        if (auctions.isEmpty())
            return "No auctions running";
        else
            return "\nNAME       HIGHEST BID    TIME LEFT\n" + auctions;
    }

    private String ls_q() {
        String queues = this.auctionHouse.listRunningQueues(this.user)
                .stream()
                .map(UniqueBidQueue.View::toString)
                .reduce("", (x, y) -> x + "\n" + y);
        if (queues.isEmpty())
            return "No queues running";
        else
            return "\nNAME       SIZE   HIGHEST BID   IS YOURS\n" + queues;

    }

    private String buy(List<String> command) {
        if (this.user == null) return Session.LOGIN_REQUIRED;
        if (command.size() < 1) return "Usage: buy " + serverTypes();
        try {
            Optional<ServerType> tp = ServerType.fromString(command.get(0));
            if (tp.isPresent()) {
                int id = this.auctionHouse.requestDroplet(tp.get(), this.user);
                return "Purchase successful! Id: " + id;
            } else {
                return "Invalid server type: " + command.get(0) + "\nAvailable server types: " + serverTypes();
            }
        } catch (DropletOfTypeWithoutStockException e) {
            return e.getMessage();
        }
    }

    private String profile() {
        Pair<Float, Float> debt = this.auctionHouse.getDebt(this.user);
        return this.user.toString() + "\n"
                + "Dropped server debt: " + String.format("%.2f", debt.getFirst()) + "\n"
                + "Running server debt: " + String.format("%.2f", debt.getSecond());
    }

    private String dropServer(List<String> command) {
        if (this.user == null) return Session.LOGIN_REQUIRED;
        if (command.size() < 1) return "Usage: drop <id>";
        try {
            int id = this.auctionHouse.dropServer(Integer.parseInt(command.get(0)), this.user);
            return "Server " + id + " dropped!";
        } catch (ServerPermissionException | ServerNotFoundException e) {
            return e.getMessage();
        } catch (NumberFormatException e) {
            return "Invalid id: " + command.get(0);
        }
    }

    private String auction(List<String> command) {
        if (this.user == null) return Session.LOGIN_REQUIRED;
        if (command.size() < 2) return "Usage: auction <amount> " + serverTypes();
        try {
            Bid b = new Bid(Float.parseFloat(command.get(0)), this.user);
            Optional<ServerType> st = ServerType.fromString(command.get(1));
            if (st.isPresent()) {
                switch (this.auctionHouse.auction(st.get(), b)) {
                    case TIMED_STARTED: return "Auction started!";
                    case TIMED_REBIDED: return "Bid added to auction!";
                    case QUEUED: return "Request queued!";
                    default: return "Something went wrong!"; /* Unreachable */
                }
            } else {
                return "Invalid server type: " + command.get(0) + "\nAvailable server types: " + serverTypes();
            }
        } catch (NumberFormatException e) {
            return "Invalid amount: " + command.get(0);
        } catch (BidTooLowException | InvalidAmountException e) {
            return e.getMessage();
        }
    }

    private static String serverTypes() {
        return Arrays.stream(ServerType.values()).map(ServerType::getName).collect(Collectors.toList()).toString();
    }

}

