package lb.microservice.product.composite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lb.microservice.api.core.product.Product;
import lb.microservice.api.core.product.ProductService;
import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.api.core.recommendation.RecommendationService;
import lb.microservice.api.core.review.Review;
import lb.microservice.api.core.review.ReviewService;
import lb.microservice.api.event.Event;
import lb.microservice.api.exceptions.InvalidInputException;
import lb.microservice.api.exceptions.NotFoundException;
import lb.microservice.util.HttpErrorInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;

import static java.util.logging.Level.FINE;
import static lb.microservice.api.event.Event.Type.CREATE;
import static lb.microservice.api.event.Event.Type.DELETE;

@Slf4j
@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private static final String URL_TEMPLATE = "http://%s:%s/%s";

    private final Scheduler publishEventScheduler;
    private final StreamBridge streamBridge;
    private final WebClient webClient;
    private final ObjectMapper mapper;
    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    @Autowired
    public ProductCompositeIntegration(@Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
                                       StreamBridge streamBridge, ObjectMapper mapper, WebClient.Builder webClientBuilder,
                                       @Value("${app.product-service.host}")
                                               String productServiceHost,
                                       @Value("${app.product-service.port}")
                                               String productServicePort,
                                       @Value("${app.recommendation-service.host}")
                                               String recommendationServiceHost,
                                       @Value("${app.recommendation-service.port}")
                                               String recommendationServicePort,
                                       @Value("${app.review-service.host}")
                                               String reviewServiceHost,
                                       @Value("${app.review-service.port}")
                                               String reviewServicePort) {

        this.streamBridge = streamBridge;
        this.publishEventScheduler = publishEventScheduler;
        this.mapper = mapper;
        this.webClient = webClientBuilder.build();
        this.productServiceUrl = String.format(URL_TEMPLATE, productServiceHost, productServicePort, "product");
        this.recommendationServiceUrl = String.format(URL_TEMPLATE, recommendationServiceHost, recommendationServicePort, "recommendation?productId=");
        this.reviewServiceUrl = String.format(URL_TEMPLATE, reviewServiceHost, reviewServicePort, "review?productId=");
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        String url = productServiceUrl + "/" + productId;
        log.debug("Will call getProduct API by URL:{}", url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Product.class)
                .log(log.getName(), FINE)
                .onErrorMap(WebClientResponseException.class, this::handleException);
    }

    @Override
    public Mono<Product> createProduct(Product body) {
        return Mono.fromCallable(() -> {
            sendMessage("products-out-0", new Event<>(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        return Mono.fromRunnable(() -> sendMessage("products-out-0", new Event<>(DELETE, productId, null)))
                .subscribeOn(publishEventScheduler)
                .then();
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        String url = recommendationServiceUrl + productId;
        log.debug("Will call getRecommendations API by URL:{}", url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(Recommendation.class)
                .log(log.getName(), FINE)
                .onErrorResume(error -> Flux.empty());
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {
        return Mono.fromCallable(() -> {
            sendMessage("recommendations-out-0", new Event<>(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteRecommendations(int productId) {
        return Mono.fromRunnable(() -> sendMessage("recommendations-out-0", new Event<>(DELETE, productId, null)))
                .subscribeOn(publishEventScheduler)
                .then();
    }

    @Override
    public Flux<Review> getReviews(int productId) {
        String url = reviewServiceUrl + productId;

        log.debug("Will call the getReviews API on URL: {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(Review.class)
                .log(log.getName(), FINE)
                .onErrorResume(error -> Mono.empty());
    }

    @Override
    public Mono<Review> createReview(Review body) {
        return Mono.fromCallable(() -> {
            sendMessage("reviews-out-0", new Event<>(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteReviews(int productId) {
        return Mono.fromRunnable(() -> sendMessage("reviews-out-0", new Event<>(DELETE, productId, null)))
                .subscribeOn(publishEventScheduler).then();
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    private Throwable handleException(WebClientResponseException wcre) {

        switch (wcre.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(wcre));

            default:
                log.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                log.warn("Error body: {}", wcre.getResponseBodyAsString());
                return wcre;
        }
    }

    private void sendMessage(String bindingName, Event<Integer, Object> event) {
        Message<Event<Integer, Object>> message = MessageBuilder.withPayload(event)
                .setHeader("partitionKey", event.getKey())
                .build();
        streamBridge.send(bindingName, message);
    }
}
