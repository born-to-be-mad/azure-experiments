package by.dma.asb.consumer;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class ConsumerService {


    private final ObjectMapper objectMapper;

    @ServiceBusListener(
            destination = "${messaging.consume.topic}",
            group = "${messaging.consume.subscription}")
    public void consume(
            String body,
            @Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT)
            ServiceBusReceivedMessageContext messageContext) {

        log.info("Consumed message[messageId={}]: {}", messageContext.getMessage().getMessageId(), body);
        try {
            var dto = objectMapper.readValue(body, MessageDto.class);
            log.info("Payload: {}", dto);
            Thread.sleep(dto.getDuration());
            if (dto.isError()) {
                log.error("An error occurred");
                throw new RuntimeException("Aggregation failed");
            }
            log.info("Message processed successfully");
        } catch (Exception e) {
            log.error("Failed to parse message body to MessageDto", e);
        }
    }
}
