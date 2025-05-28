package by.dma.asb.consumer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import static java.lang.System.nanoTime;
import static java.lang.System.out;
import static java.util.concurrent.CompletableFuture.runAsync;

@Service
@Slf4j
@AllArgsConstructor
@ConditionalOnProperty(
        name = "messaging.consume.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ConsumerService {

    private final ObjectMapper objectMapper;

    @ServiceBusListener(
            destination = "${messaging.consume.topic}",
            group = "${messaging.consume.subscription}",
            concurrency = "${messaging.consume.concurrency:5}"
    )
    public void consume(
            String body,
            @Header(ServiceBusMessageHeaders.RECEIVED_MESSAGE_CONTEXT)
            ServiceBusReceivedMessageContext messageContext) throws InterruptedException {

        String messageId = messageContext.getMessage().getMessageId();
        long retryAttempt = messageContext.getMessage().getDeliveryCount();
        log.info("Message[{}] consumption started, retry attempt={}, payload={}",
                messageId, retryAttempt, body);
        try {
            long time = nanoTime();
            var dto = getMessageDto(body);
            log.info("Processing the payload: {}", dto);
            int duration = dto.getDuration();
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List.of(
                        runAsync(() -> processing("Aggregate order-data", duration), executor),
                        runAsync(() -> processing("Aggregate department-data", duration, dto.isError()), executor),
                        runAsync(() -> processing("Aggregate questionnaire-data", duration), executor),
                        runAsync(() -> processing("Aggregate payment-data", duration), executor)
                ).forEach(CompletableFuture::join);
            }
            messageContext.complete();
            time = nanoTime() - time;
            log.info("Message[{}] processed successfully in {}", messageId, (time / 1_000_000));
        } catch (NotRetryableException e) {
            log.debug("Dead lettering message[messageId={}]", messageId);
            moveToDlq(messageContext, e);
        }
    }

    private void processing(String operation, int baseDuration) {
        processing(operation, baseDuration, false);
    }

    private void processing(String operation, int baseDuration, boolean error) {
        var duration = baseDuration * ThreadLocalRandom.current().nextInt(100, 150) / 100;
        log.info("{}...expected duration={}", operation, duration);
        try {
            Thread.sleep(duration);
            if (error) {
                String errorMessage = "%s failed".formatted(operation);
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
