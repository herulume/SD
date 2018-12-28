package server.exception;

public class NoRunningAuctionsException extends Exception {
    public NoRunningAuctionsException(String message) {
        super(message);
    }
}
