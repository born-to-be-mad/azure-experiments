package by.dma.asb.consumer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import by.dma.asb.producer.ProducerService;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.azure.spring.messaging.servicebus.implementation.core.annotation.ServiceBusListener;
import com.azure.spring.messaging.servicebus.support.ServiceBusMessageHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import static java.lang.System.nanoTime;
import static java.util.concurrent.CompletableFuture.runAsync;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "messaging.consume.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ConsumerService {

    private final ObjectMapper objectMapper;

    @Value("${messaging.produce.out-topic}")
    private String topic;

    private final ProducerService messagingProducerService;

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
            int baseDuration = dto.getDuration();
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List.of(
                        runAsync(() -> processingWithNotRetryable("Aggregate order-data", baseDuration), executor),
                        runAsync(() -> processing("Aggregate department-data", baseDuration, dto.isError()), executor),
                        runAsync(() -> processing("Aggregate questionnaire-data", baseDuration), executor),
                        runAsync(() -> processing("Aggregate payment-data", baseDuration), executor)
                ).forEach(CompletableFuture::join);
            }
            sendOutputMessage(dto);
            messageContext.complete();
            time = nanoTime() - time;
            log.info("Message[{}] processed successfully in {}", messageId, (time / 1_000_000));
        } catch (Exception e) {
            if (e instanceof NotRetryableException nre) {
                log.error("Message[{}] processing failed with unrecoverable error: {}", messageId, nre.getMessage());
                moveToDlq(messageContext, nre);
            } else if (e.getCause() instanceof NotRetryableException nre) {
                log.error("Message[{}] processing failed with unrecoverable error: {}", messageId, nre.getMessage());
                moveToDlq(messageContext, nre);
            } else {
                log.warn("Message[{}] processing failed, retrying...", messageId, e);
                messageContext.abandon();
            }
        }
    }

    private void sendOutputMessage(MessageDto dto) {
        try {
            messagingProducerService.send(topic, objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", dto, e);
            throw new NotRetryableException("Failed to serialize message", e);
        }
    }

    private void processing(String operation, int baseDuration) {
        processing(operation, baseDuration, false);
    }

    private void processingWithNotRetryable(String operation, int baseDuration) {
        boolean errorOccurred = baseDuration % 13 == 0;
        if (errorOccurred) {
            processingWithError(operation, baseDuration, new NotRetryableException("Simulated unrecoverable error"));
        } else {
            processing(operation, baseDuration);
        }
    }

    private void processing(String operation, int baseDuration, boolean error) {
        processingWithError(operation, baseDuration, error ? new RuntimeException("Simulated error") : null);
    }

    private void processingWithError(String operation, int baseDuration, RuntimeException exception) {
        var duration = baseDuration * ThreadLocalRandom.current().nextInt(100, 170) / 100;
        log.info("{}...expected duration={}", operation, duration);
        try {
            Thread.sleep(duration);
            if (exception != null) {
                String errorMessage = "%s failed: %s".formatted(operation, exception.getMessage());
                log.error(errorMessage);
                throw exception;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void moveToDlq(ServiceBusReceivedMessageContext messageContext, Exception e) {
        log.error("Moving message[id={}] to DLQ due to unrecoverable error: {}",
                messageContext.getMessage().getMessageId(), e.getMessage());
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
