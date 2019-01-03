package server;

import org.junit.Assert;
import util.AtomicInt;

import java.io.*;
import java.net.Socket;

public class ServerTest implements Runnable {

    private static AtomicInt ID = new AtomicInt(0);

    private final int id;
    private BufferedReader reader;
    private PrintWriter writer;

    public static void main(String[] args) {
        final int num_threads = 1000;
        Thread[] threads = new Thread[num_threads];
        for (int i = 1; i < num_threads; i++) {
            threads[i] = new Thread(new ServerTest());
            threads[i].start();
        }
        new ServerTest().run();
        for (int i = 1; i < num_threads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ServerTest() {
        this.id = ServerTest.ID.fetchAndApply(x -> x + 1);
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
        String response;
        String message;
        try {
            message = String.format("register %d %d %d", this.id, this.id, this.id);
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertEquals(response, "Registered successfully");
            message = String.format("login %d %d", this.id, this.id);
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertEquals(response, "Logged in");
            message = String.format("auction %d ts1.lower", this.id % 30);
            this.writer.println(message);
            response = this.reader.readLine();
            Assert.assertTrue(this.toString() + " " + message + " " + response,
                    response.equals("Auction started!")
                    || response.equals("Bid added to auction!")
                    || response.equals("Request queued!")
                    || response.contains("Please bid higher then the current highest bid:"));
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Override
    public String toString() {
        return this.id + "";
    }
}