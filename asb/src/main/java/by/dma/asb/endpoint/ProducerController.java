package by.dma.asb.endpoint;

import by.dma.asb.producer.ProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produce")
@RequiredArgsConstructor
public class ProducerController {

    private final ProducerService consoleProducerService;

    @PostMapping
    public void sendMessage(@RequestBody String message) {
        consoleProducerService.send(message);
    }
}

