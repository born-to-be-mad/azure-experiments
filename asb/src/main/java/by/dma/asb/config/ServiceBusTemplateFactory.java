package by.dma.asb.config;

import com.azure.spring.cloud.service.servicebus.properties.ServiceBusEntityType;
import com.azure.spring.messaging.servicebus.core.ServiceBusTemplate;

public interface ServiceBusTemplateFactory {

    ServiceBusTemplate newTemplate(ServiceBusEntityType entityType);
}
