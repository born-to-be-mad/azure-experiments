package by.dma.asb;

import com.azure.spring.messaging.implementation.annotation.EnableAzureMessaging;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAzureMessaging
public class AsbApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsbApplication.class, args);
    }

}
