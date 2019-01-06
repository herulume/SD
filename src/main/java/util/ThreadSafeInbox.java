package util;

import java.util.Deque;
import java.util.LinkedList;

public class ThreadSafeInbox {

    private final Deque<String> messages;

    public ThreadSafeInbox() {
        this.messages = new LinkedList<>();
    }

    synchronized public void write(String message) {
        this.messages.push(message);
        this.notifyAll();
    }

    synchronized public String read() throws InterruptedException {
        while (this.messages.isEmpty()) {
            this.wait();
        }
        return messages.poll();
    }
}
