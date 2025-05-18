package by.dma.asb.producer;

import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Slf4j
@Service
public class MessagingProducerService implements ProducerService {

    private final ServiceBusTemplate serviceBusTemplate;

    @Override
    public void send(String payload) {
        log.info("Sending message: {}", payload);
        Message<String> message = MessageBuilder
                .withPayload(payload)
                //.setHeader(ServiceBusMessageHeaders.SCHEDULED_ENQUEUE_TIME, offsetDuration)
                .build();
        serviceBusTemplate.send("test-topic", message);
    }
}
