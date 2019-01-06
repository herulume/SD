package server;

import util.ThreadSafeInbox;

import java.util.Objects;

public class User {

    private final String name;
    private final String email; // Unique
    private final String password;
    private final ThreadSafeInbox messages;

    User(String name, String email, String password) {
        this.name = Objects.requireNonNull(name);
        this.email = Objects.requireNonNull(email);
        this.password = Objects.requireNonNull(password);
        this.messages = new ThreadSafeInbox();
    }

    public String getEmail() {
        return this.email;
    }

    boolean authenticate(String password) {
        return this.password.equals(password);
    }

    void sendNotification(String message) {
        this.messages.write(message);
    }

    public String readNotification() throws InterruptedException {
        return this.messages.read();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(email, user.email);
    }

    @Override
    public String toString() {
        return "name: " + this.name + '\n' + "email: " + this.email;
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
