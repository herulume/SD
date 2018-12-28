package server;

import server.middleware.Session;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int port = 5000;

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(port);
        AuctionHouse auctionHouse = new AuctionHouse();

        while (true) {
            Socket clientSocket = server.accept();
            new Thread(new Session(clientSocket, auctionHouse)).start();
        }
    }
}
