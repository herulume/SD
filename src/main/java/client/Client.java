package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import static java.lang.System.err;
import static java.lang.System.out;

public class Client {

    private Socket s;
    private BufferedReader reader;
    private PrintWriter writer;
    private Scanner console;

    public static void main(String[] args){
        try{
            new Client("localhost", 5000).run();
        } catch (IOException e) {
            err.println(e.getMessage());
        }
    }

    private Client(String address, int port) throws IOException {
        this.s = new Socket(address, port);
        this.reader = new BufferedReader(new InputStreamReader(this.s.getInputStream()));
        this.writer = new PrintWriter(new OutputStreamWriter(this.s.getOutputStream()), /*auto flush*/ true);
        this.console = new Scanner(System.in);
    }

    private void run() {
        new Thread(this::inbox).start();
        try {
            for (; ; ) {
                String message = this.console.nextLine().trim();
                this.writer.println(message);
                if (message.equals("quit")) break;
            }
        } finally {
            gracefulShutdown();
        }
    }

    private void inbox() {
        String serverMessage;
        try {
            while ((serverMessage = this.reader.readLine()) != null)
                if (!serverMessage.isEmpty()) out.println(serverMessage);
        } catch (IOException e) {
            err.println(e.getMessage());
        } finally {
            gracefulShutdown();
        }
    }

    private void gracefulShutdown() {
        if (this.console != null)
            console.close();
        if (this.writer != null)
            this.writer.close();
        try {
            if (this.reader != null)
                this.reader.close();
            if (this.s != null)
                this.s.close();
        } catch (IOException ignored) {
        }
    }
}
