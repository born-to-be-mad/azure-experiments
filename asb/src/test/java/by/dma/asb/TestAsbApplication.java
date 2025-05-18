package by.dma.asb;

import org.springframework.boot.SpringApplication;

public class TestAsbApplication {

    public static void main(String[] args) {
        SpringApplication.from(AsbApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
