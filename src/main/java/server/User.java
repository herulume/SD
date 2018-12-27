package server;

import util.ThreadSafeInbox;

import java.util.Objects;

public class User {
    private final String name;
    private final String email; // Unique
    private final String password;
    private ThreadSafeInbox messages;

    User(String name, String email, String password) {
        this.name = Objects.requireNonNull(name);
        this.email = Objects.requireNonNull(email);
        this.password = Objects.requireNonNull(password);
        this.messages = new ThreadSafeInbox();
    }

    String getName() {
        return this.name;
    }

    boolean authenticate(String password) {
        return this.password.equals(password);
    }

    void sendNotification(String message) {
        this.messages.write(message);
    }

    String readNotification() throws InterruptedException {
        return this.messages.read();
    }

    void reset() {
        this.messages.reset();
    }


    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o == null || (this.getClass() != o.getClass()))
            return false;

        User usr = (User) o;
        return this.email.equals(usr.email);
    }
}
