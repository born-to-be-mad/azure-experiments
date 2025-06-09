package by.dma.asb.consumer;

public class NotRetryableException extends RuntimeException {
    public NotRetryableException(String message) {
        super(message);
    }

    public NotRetryableException(String message, Exception e) {
        super(message, e);
    }
}
