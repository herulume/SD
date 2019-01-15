package server.middleware;

import server.User;

import java.io.PrintWriter;
import java.util.Objects;

class Ctt implements Runnable {

    private final PrintWriter outputInbox;
    private final User user;

    Ctt(User user, PrintWriter ob) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(ob);

        this.user = user;
        this.outputInbox = ob;
    }

    public void run() {
        while (true) {
            try {
                String message = user.readNotification();
                outputInbox.println("[*] New message: " + message + "\n");
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
