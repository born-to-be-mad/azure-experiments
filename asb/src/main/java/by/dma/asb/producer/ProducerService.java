package by.dma.asb.producer;

public interface ProducerService {
    void send(String topic, String payload);
}
