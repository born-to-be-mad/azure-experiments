package by.dma.asb.endpoint;

import by.dma.asb.producer.ProducerService;
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

    @PostMapping
    public void sendMessage(@RequestBody String message) {
        messagingProducerService.send(topic, message);
    }
}

