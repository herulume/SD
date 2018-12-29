package server.middleware;

import server.*;
import server.exception.*;
import util.Pair;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Session implements Runnable {

    // Common error messages
    private static final String LOGIN_REQUIRED = "You must be logged in to use this command";

    private User user;
    private Socket socket;
    private AuctionHouse auctionHouse;

    public Session(Socket socket, AuctionHouse auctionHouse){
        this.socket = socket;
        this.auctionHouse = auctionHouse;
        this.user = null;
    }

    @Override
    public void run(){
        BufferedReader in = null;
        PrintWriter out = null;
        try{
            try{
                out = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream()),/*auto flush*/true);
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            }catch(IOException e){
                System.out.println("Couldn't set up IO: " + e.getMessage());
                return;
            }
            String input;
            while((input = in.readLine()) != null){
                List<String> command = Arrays.stream(input.split(" "))
                        .filter(s -> s.length() != 0)
                        .collect(Collectors.toList());
                if(command.size() == 0){
                    out.println();
                    continue;
                }
                if(command.get(0).equals("quit")) break;
                String response = runCommand(command);
                out.println(response);
            }
        }catch(IOException e){
            System.out.println("Connection closed by client (User: " + this.user + "): " + e.getMessage());
        }finally{
            if(out != null){
                out.close();
            }
            try{
                if(in != null){
                    in.close();
                }
                this.socket.close();
            }catch(IOException ignored){
            }
        }
    }

    private String runCommand(List<String> command){
        switch(command.get(0)){
            case "register":
                return register(command.subList(1, command.size()));
            case "login":
                return login(command.subList(1, command.size()));
            case "ls":
                return ls(command.subList(1, command.size()));
            case "buy":
                return buy(command.subList(1, command.size()));
            case "profile":
                return profile();
            case "drop":
                return dropServer(command.subList(1, command.size()));
            case "auction":
                return auction(command.subList(1, command.size()));
            case "bid":
                return bid(command.subList(1, command.size()));
            default:
                return "Command not found: " + command.get(0);
        }
    }

    private String register(List<String> command){
        if(command.size() < 3) return "Usage: register <email> <username> <password>";
        try{
            this.auctionHouse.signUp(command.get(0), command.get(2), command.get(1));
            return "Registered successfully";
        }catch(UserAlreadyExistsException e){
            return e.getMessage();
        }
    }

    private String login(List<String> command){
        if(command.size() < 2) return "Usage: login <email> <password>";
        try{
            this.user = this.auctionHouse.login(command.get(0), command.get(1));
            return "Logged in";
        }catch(LoginException e){
            return e.getMessage();
        }
    }

    private String ls(List<String> command){
        if(this.user == null) return Session.LOGIN_REQUIRED;
        if(command.size() == 0){
            return this.auctionHouse.listAvailableServers()
                    .stream()
                    .map(ServerType::getName)
                    .reduce("", (x, y) -> x + "\n" + y);
        }
        if(command.get(0).equals("-m")){
            Pair<String,String> possessions = this.auctionHouse.listOwnedServers(this.user.getEmail())
                    .mapFirst(x -> x.stream().map(Auction::toString).collect(Collectors.joining("\n")))
                    .mapSecond(x -> x.stream().map(Droplet::toString).collect(Collectors.joining("\n")))
                    .swap();
            StringBuilder result = new StringBuilder("DROPLETS:\n");
            if(possessions.getFirst().isEmpty()){
                result.append("You have no droplets\n");
            }else{
                result.append("ID\tNAME\n=======================\n")
                        .append(possessions.getFirst())
                        .append("\n");
            }
            result.append("AUCTIONED DROPLETS:\n");
            if(possessions.getSecond().isEmpty()){
                result.append("You have no auctioned droplets\n");
            }else{
                result.append("\nID\tNAME\tPRICE PAID\n=======================\n")
                        .append(possessions.getSecond())
                        .append("\n");
            }
            return result.toString();
        }
        if(command.get(0).equals("-a")){
            return "\nID\tNAME\tHIGHEST BID\n=======================\n" +
                    this.auctionHouse.listRunningAuctions()
                            .stream()
                            .map(Auction::toString)
                            .reduce("", (x, y) -> x + "\n" + y);
        }
        return "Usage: ls [OPTION]\n\t-m show my droplets\n\t-a show available auctions";
    }

    private String buy(List<String> command){
        if(this.user == null) return Session.LOGIN_REQUIRED;
        if(command.size() < 1) return "Usage: buy " + serverTypes();
        try{
            int id = this.auctionHouse.requestDroplet(ServerType.valueOf(command.get(0)), this.user);
            return "Purchase successful! Id: " + id;
        }catch(DropletOfTypeWithoutStock e){
            return e.getMessage();
        }
    }

    private String profile(){
        return this.user.toString();
    }

    private String dropServer(List<String> command){
        if(this.user == null) return Session.LOGIN_REQUIRED;
        if(command.size() < 1) return "Usage: drop <id>";
        try{
            int id = this.auctionHouse.dropServer(Integer.parseInt(command.get(0)), this.user);
            return "Server " + id + " dropped!";
        }catch(ServerPermissionException | ServerNotFound e){
            return e.getMessage();
        }catch(NumberFormatException e){
            return "Invalid id: " + command.get(0);
        }
    }

    private String auction(List<String> command){
        if(this.user == null) return Session.LOGIN_REQUIRED;
        if(command.size() < 2) return "Usage: auction <amount> " + serverTypes();
        try{
            Bid b = new Bid(this.user, Float.parseFloat(command.get(0)));
            int id = this.auctionHouse.startAuction(ServerType.valueOf(command.get(1)), b);
            return "Auction started: " + id;
        }catch(AuctionOfTypeRunningException e){
            return e.getMessage();
        }catch(NumberFormatException e){
            return "Invalid amount: " + command.get(0);
        }
    }

    private String bid(List<String> command){
        if(this.user == null) return Session.LOGIN_REQUIRED;
        if(command.size() < 2) return "Usage: <amount> <id>";
        try{
            Bid b;
            try{
                b = new Bid(this.user, Float.parseFloat(command.get(0)));
            }catch(NumberFormatException e){
                return "Invalid amount: " + command.get(0);
            }
            this.auctionHouse.bid(Integer.parseInt(command.get(1)), b);
            return "Bid placed!";
        }catch(NumberFormatException e){
            return "Invalid id: " + command.get(1);
        }catch(InvalidAuctionException | NoRunningAuctionsException | LowerBidException e){
            return e.getMessage();
        }
    }

    private static String serverTypes(){
        return Arrays.stream(ServerType.values()).map(ServerType::getName).collect(Collectors.toList()).toString();
    }
}

