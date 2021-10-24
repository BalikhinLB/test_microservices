package lb.microservice.product.service;

import lb.microservice.api.core.product.Product;
import lb.microservice.api.core.product.ProductService;
import lb.microservice.api.event.Event;
import lb.microservice.api.exceptions.EventProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class MessageProcessorConfig {
    private final ProductService productService;

    @Autowired
    public MessageProcessorConfig(ProductService productService) {
        this.productService = productService;
    }

    @Bean
    public Consumer<Event<Integer, Product>> messageProcessor(){
        return event -> {
            log.info("Process message with key:{} created at {}", event.getKey(), event.getEventCreatedAt());
            switch (event.getEventType()) {
                case CREATE -> {
                    log.info("Create a product with id:{}", event.getKey());
                    productService.createProduct(event.getData()).block();
                }
                case DELETE -> {
                    log.info("Delete product with id:{}", event.getKey());
                    productService.deleteProduct(event.getKey()).block();
                }
                default -> {
                    String errorMessage = String.format("Incorrect event type: %s , expected a CREATE or DELETE event", event.getEventType());
                    throw new EventProcessingException(errorMessage);
                }
            }
            log.info("Message processing done!");
        };
    }
}
