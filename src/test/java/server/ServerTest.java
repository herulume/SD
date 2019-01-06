package server;

import org.junit.Assert;
import util.AtomicInt;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ServerTest implements Runnable {

    private static AtomicInt ID = new AtomicInt(0);

    private final int id;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean auctioning;

    public static void main(String[] args) {
        final int num_threads = 1000;
        Thread[] threads = new Thread[num_threads];
        for (int i = 1; i < num_threads; i++) {
            threads[i] = new Thread(new ServerTest(i % 2 == 0));
            threads[i].start();
        }
        new ServerTest(true).run();
        for (int i = 1; i < num_threads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ServerTest(boolean auctioning) {
        this.id = ServerTest.ID.fetchAndApply(x -> x + 1);
        this.auctioning = auctioning;
        try {
            Socket s = new Socket("localhost", 5000);
            this.reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), /*auto flush*/ true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (this.auctioning) {
            auction();
        } else {
            buyAndDrop();
        }
    }

    private void auction() {
        String response;
        String message;
        try {
            message = String.format("register %d %d %d", this.id, this.id, this.id);
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertTrue(response.equals("Email already registered") || response.equals("Registered successfully"));
            message = String.format("login %d %d", this.id, this.id);
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertEquals(response, "Logged in");
            message = String.format("auction %d ts1.lower", this.id % 30);
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertTrue(this.toString() + " | " + message + " | " + response,
                    response.equals("Auction started!")
                            || response.equals("Bid added to auction!")
                            || response.equals("Request queued!")
                            || response.contains("Please bid higher then the current highest bid:")
                            || response.contains("New message"));
            if (response.contains("Please bid higher")) {
                int bid = (int) Float.parseFloat(response.substring("Please bid higher then the current highest bid:".length()).trim());
                message = String.format("auction %d ts1.lower", bid + 1);
                this.writer.println(message);
                response = this.reader.readLine();
                Assert.assertTrue(this.toString() + " | " + message + " | " + response,
                        response.equals("Bid added to auction!")
                                || response.contains("Please bid higher then the current highest bid:")
                                || response.contains("New message"));
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    private void buyAndDrop() {
        String response;
        String message;
        try {
            message = String.format("register %d %d %d", this.id, this.id, this.id);
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertTrue(response.equals("Email already registered") || response.equals("Registered successfully"));
            message = String.format("login %d %d", this.id, this.id);
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertEquals(response, "Logged in");
            message = "buy ts2.high";
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertTrue(response, response.contains("Purchase successful!") || response.equals("No stock for type: ts2.high"));
            message = "ls -m";
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertTrue(response.contains("DROPLETS:"));
            if (!response.contains("You have no droplets")) {
                response = this.reader.readLine();
                Assert.assertTrue(response, response.contains("ID") && response.contains("NAME"));
                response = this.reader.readLine();
                Assert.assertTrue(response.contains("ts2.high"));
                List<String> droplet = Arrays.stream(response.split(" ")).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                this.reader.readLine();
                this.reader.readLine();
                message = String.format("drop %d", Integer.parseInt(droplet.get(0)));
                this.writer.println(message);
                response = this.reader.readLine();
                Assert.assertEquals("Server " + Integer.parseInt(droplet.get(0)) + " dropped!", response);
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "id: " + this.id;
    }
}