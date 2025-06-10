package by.dma.asb.endpoint;

import java.util.UUID;

import by.dma.asb.consumer.MessageDto;
import by.dma.asb.consumer.NotRetryableException;
import by.dma.asb.producer.ProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produce")
@RequiredArgsConstructor
public class ProducerController {

    @Value("${messaging.produce.in-topic}")
    private String topic;

    private final ProducerService messagingProducerService;

    private final ObjectMapper objectMapper;

    @PostMapping
    public void sendMessage(@RequestBody String message) {
        var dto = getMessageDto(message);
        String messageId = dto.getId();
        messagingProducerService.send(topic, messageId, message);
    }

    private MessageDto getMessageDto(String body) {
        try {
            return objectMapper.readValue(body, MessageDto.class);
        } catch (Exception e) {
            throw new NotRetryableException("Failed to parse message", e);
        }
    }
}

