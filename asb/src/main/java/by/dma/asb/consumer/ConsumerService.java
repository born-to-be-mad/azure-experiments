package by.dma.asb.consumer;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
            ServiceBusReceivedMessageContext messageContext) throws InterruptedException {

        String messageId = messageContext.getMessage().getMessageId();
        long retryAttempt = messageContext.getMessage().getDeliveryCount();
        log.info("Message[{}] consumption started, retry attempt={}, payload={}",
                messageId, retryAttempt, body);
        try {
            var dto = getMessageDto(body);
            log.info("Processing the payload: {}", dto);
            Thread.sleep(dto.getDuration());
            if (dto.isError()) {
                String errorMessage = "Message[%s] aggregation failed".formatted(messageId);
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
            messageContext.complete();
            log.info("Message[{}] processed successfully", messageId);
        } catch (NotRetryableException e) {
            log.debug("Dead lettering message[messageId={}]", messageId);
            moveToDlq(messageContext, e);
        }
    }

    private void moveToDlq(ServiceBusReceivedMessageContext messageContext, NotRetryableException e) {
        DeadLetterOptions options = new DeadLetterOptions();
        options.setDeadLetterReason(e.getMessage());
        options.setDeadLetterErrorDescription(getErrorDescription(e));
        messageContext.deadLetter(options);
    }

    private MessageDto getMessageDto(String body) {
        try {
            return objectMapper.readValue(body, MessageDto.class);
        } catch (Exception e) {
            throw new NotRetryableException("Failed to parse message", e);
        }
    }

    private String getErrorDescription(Exception e) {
        String description = StringUtils.defaultIfBlank(ExceptionUtils.getStackTrace(e), null);
        return StringUtils.substring(description, 0, 2000);
    }
}
