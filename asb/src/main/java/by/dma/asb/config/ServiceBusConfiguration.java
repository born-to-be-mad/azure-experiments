package by.dma.asb.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.spring.cloud.core.customizer.AzureServiceClientBuilderCustomizer;
import com.azure.spring.cloud.service.servicebus.properties.ServiceBusEntityType;
import com.azure.spring.messaging.servicebus.core.ServiceBusProducerFactory;
import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ServiceBusConfiguration {

    @Value("${messaging.produce.concurrency:5}")
    private Integer concurrency;

    @Bean
    public ServiceBusTemplateFactory serviceBusTemplateFactory(ServiceBusProducerFactory serviceBusProducerFactory) {
        return entityType -> {
            ServiceBusTemplate template = new ServiceBusTemplate(serviceBusProducerFactory);
            template.setDefaultEntityType(entityType);
            return template;
        };
    }

    @Bean
    public ServiceBusTemplate serviceBusQueueTemplate(ServiceBusTemplateFactory serviceBusTemplateFactory) {
        return serviceBusTemplateFactory.newTemplate(ServiceBusEntityType.QUEUE);
    }

    @Bean
    public AzureServiceClientBuilderCustomizer<
            ServiceBusClientBuilder.ServiceBusProcessorClientBuilder> serviceBusProcessorClientBuilderCustomizer() {

        log.info("ServiceBusProcessorClientBuilderCustomizer: concurrency={}", concurrency);
        /*
         * Workaround: this behavior is supposed to be controlled by
         * "spring.cloud.azure.servicebus.processor.auto-complete" property, but for reason unknown it gets ignored.
         */
        return serviceBusProcessorClientBuilder ->
                serviceBusProcessorClientBuilder
                        .disableAutoComplete()
                        .maxConcurrentCalls(concurrency)
                        .receiveMode(ServiceBusReceiveMode.PEEK_LOCK);
    }
}
