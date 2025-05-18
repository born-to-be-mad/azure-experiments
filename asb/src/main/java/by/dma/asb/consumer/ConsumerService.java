package by.dma.asb.consumer;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConsumerService {

    @ServiceBusListener(
            destination = "test-topic",
            group = "test-subscription")
    public void consume(
            String body,
            @Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT)
            ServiceBusReceivedMessageContext messageContext) {

        log.info("Consumed message[messageId={}]: {}", messageContext.getMessage().getMessageId(), body);
    }
}
