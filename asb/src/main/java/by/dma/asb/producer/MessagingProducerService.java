package by.dma.asb.producer;

import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MessagingProducerService implements ProducerService {

    private final ServiceBusTemplate serviceBusTemplate;

    public MessagingProducerService(ServiceBusTemplate serviceBusTemplate) {
        this.serviceBusTemplate = serviceBusTemplate;
    }

    /**
     * Sends a message to the specified topic with the given message ID and payload.
     *
     * @param topic     the topic to send the message to
     * @param messageId The MessageId can always be some GUID,
     *                  but anchoring the identifier to the business process
     *                  yields predictable repeatability,
     *                  which is desired for using the duplicate detection feature effectively.
     *                  See https://learn.microsoft.com/en-us/azure/service-bus-messaging/duplicate-detection#how-it-works
     * @param payload   the content of the message
     */
    @Override
    public void send(String topic, String messageId, String payload) {
        log.info("Sending message to '{}'. Payload: {}", topic, payload);
        Message<String> message = MessageBuilder
                .withPayload(payload)
                //.setHeader(ServiceBusMessageHeaders.SCHEDULED_ENQUEUE_TIME, offsetDuration)
                .setHeader(ServiceBusMessageHeaders.MESSAGE_ID, messageId)
                .build();
        serviceBusTemplate.send(topic, message);
    }
}


