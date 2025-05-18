package by.dma.asb.consumer;

import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import org.springframework.stereotype.Service;

@Service
public class ConsumerService {
    private static final String QUEUE_NAME = "que001";

    @ServiceBusListener(destination = QUEUE_NAME)
    public void handleMessageFromServiceBus(String message) {
        System.out.printf("Consume message: %s%n", message);
    }
}
