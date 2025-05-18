package by.dma.asb.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConsoleProducerService implements ProducerService {

    @Override
    public void send(String payload) {
      log.info("Sending message: {}", payload);
    }
}
