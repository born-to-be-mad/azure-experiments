package by.dma.asb.producer;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ProducerService {

    private ServiceBusSenderClient senderClient;

    public void sendMessage(String message) {
        senderClient.sendMessage(new ServiceBusMessage(message));
        log.info("Message sent: {}", message);
    }
}
