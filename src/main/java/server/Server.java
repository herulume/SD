package server;

import server.middleware.Session;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int port = 5000;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(port);
        AuctionHouse auctionHouse = new AuctionHouse();
        ExecutorService pool = Executors.newCachedThreadPool();

        System.out.println("Server started");
        while (true) {
            Socket clientSocket = server.accept();
            System.out.println(LocalDateTime.now() + " : Connection accepted");
            pool.submit((new Session(clientSocket, auctionHouse)));
        }
    }
}
