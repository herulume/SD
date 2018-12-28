package util;

import java.util.ArrayList;

public class ThreadSafeInbox {
    private ArrayList<String> messages;
    private int index;

    public ThreadSafeInbox() {
        this.messages = new ArrayList<>();
        index = 0;
    }

    synchronized public void write(String message) {
        this.messages.add(message);
        notifyAll();
    }

    synchronized public String read() throws InterruptedException {
        while (this.isEmpty()) {
            wait();
        }

        String message = messages.get(index);
        index += 1;

        return message;
    }

    synchronized public void reset() {
        this.index = 0;
    }

    synchronized private boolean isEmpty() {
        return this.messages.size() == this.index;
    }
}
