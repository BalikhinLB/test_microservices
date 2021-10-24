package lb.microservice.recommendation.service;

import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.api.core.recommendation.RecommendationService;
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

    private final RecommendationService recommendationService;

    @Autowired
    public MessageProcessorConfig(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Bean
    public Consumer<Event<Integer, Recommendation>> messageProcessor() {
        return event -> {

            log.info("Process message created at {}...", event.getEventCreatedAt());

            switch (event.getEventType()) {
                case CREATE -> {
                    Recommendation recommendation = event.getData();
                    log.info("Create recommendation with ID: {}/{}", recommendation.getProductId(), recommendation.getRecommendationId());
                    recommendationService.createRecommendation(recommendation).block();
                }
                case DELETE -> {
                    int productId = event.getKey();
                    log.info("Delete recommendations with ProductID: {}", productId);
                    recommendationService.deleteRecommendations(productId).block();
                }
                default -> {
                    String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or DELETE event";
                    log.warn(errorMessage);
                    throw new EventProcessingException(errorMessage);
                }
            }

            log.info("Message processing done!");
        };
    }
}
