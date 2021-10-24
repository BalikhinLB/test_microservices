package lb.microservice.product.composite;

import lb.microservice.api.composite.product.ProductAggregate;
import lb.microservice.api.composite.product.RecommendationSummary;
import lb.microservice.api.composite.product.ReviewSummary;
import lb.microservice.api.core.product.Product;
import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.api.core.review.Review;
import lb.microservice.api.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static lb.microservice.api.event.Event.Type.CREATE;
import static lb.microservice.api.event.Event.Type.DELETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.ACCEPTED;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import({TestChannelBinderConfiguration.class})
class MessagingTest {

    @Autowired
    private WebTestClient client;

    @SuppressWarnings({"Sp", "SpringJavaInjectionPointsAutowiringInspection"})
    @Autowired
    private OutputDestination target;

    @BeforeEach
    void setUp() {
        purgeMessages("products");
        purgeMessages("recommendations");
        purgeMessages("reviews");
    }

    @Test
    void createCompositeProduct1(){
        ProductAggregate composite = new ProductAggregate(1, "name", 1, null, null, null);
        postAndVerifyProduct(composite, ACCEPTED);

        final List<String> productMessages = getMessages("products");
        final List<String> recommendationMessages = getMessages("recommendations");
        final List<String> reviewMessages = getMessages("reviews");

        assertEquals(1, productMessages.size());
        assertEquals(0, recommendationMessages.size());
        Event<Integer, Product> expectedEvent =
        	      new Event<>(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));

        MatcherAssert.assertThat(productMessages.get(0), Matchers.is(sameEventExceptCreatedAt(expectedEvent)));
        
        assertEquals(0, reviewMessages.size());
    }

    @Test
    void createCompositeProduct2() {

        ProductAggregate composite = new ProductAggregate(1, "name", 1,
                singletonList(new RecommendationSummary(1, "a", 1, "c")),
                singletonList(new ReviewSummary(1, "a", "s", "c")), null);
        postAndVerifyProduct(composite, ACCEPTED);

        final List<String> productMessages = getMessages("products");
        final List<String> recommendationMessages = getMessages("recommendations");
        final List<String> reviewMessages = getMessages("reviews");

        // Assert one create product event queued up
        assertEquals(1, productMessages.size());

        Event<Integer, Product> expectedProductEvent =
                new Event(CREATE, composite.getProductId(), new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null));
        MatcherAssert.assertThat(productMessages.get(0), Matchers.is(sameEventExceptCreatedAt(expectedProductEvent)));

        // Assert one create recommendation event queued up
        assertEquals(1, recommendationMessages.size());

        RecommendationSummary rec = composite.getRecommendations().get(0);
        Event<Integer, Product> expectedRecommendationEvent =
                new Event(CREATE, composite.getProductId(),
                        new Recommendation(composite.getProductId(), rec.getRecommendationId(), rec.getAuthor(), rec.getRate(), rec.getContent(), null));
        MatcherAssert.assertThat(recommendationMessages.get(0), Matchers.is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

        // Assert one create review event queued up
        assertEquals(1, reviewMessages.size());

        ReviewSummary rev = composite.getReviews().get(0);
        Event<Integer, Product> expectedReviewEvent =
                new Event(CREATE, composite.getProductId(), new Review(composite.getProductId(), rev.getReviewId(), rev.getAuthor(), rev.getSubject(), rev.getContent(), null));
        MatcherAssert.assertThat(reviewMessages.get(0), Matchers.is(sameEventExceptCreatedAt(expectedReviewEvent)));
    }

    @Test
    void deleteCompositeProduct() {
        deleteAndVerifyProduct(1, ACCEPTED);

        final List<String> productMessages = getMessages("products");
        final List<String> recommendationMessages = getMessages("recommendations");
        final List<String> reviewMessages = getMessages("reviews");

        // Assert one delete product event queued up
        assertEquals(1, productMessages.size());

        Event<Integer, Product> expectedProductEvent = new Event(DELETE, 1, null);
        MatcherAssert.assertThat(productMessages.get(0), Matchers.is(sameEventExceptCreatedAt(expectedProductEvent)));

        // Assert one delete recommendation event queued up
        assertEquals(1, recommendationMessages.size());

        Event<Integer, Product> expectedRecommendationEvent = new Event(DELETE, 1, null);
        MatcherAssert.assertThat(recommendationMessages.get(0), Matchers.is(sameEventExceptCreatedAt(expectedRecommendationEvent)));

        // Assert one delete review event queued up
        assertEquals(1, reviewMessages.size());

        Event<Integer, Product> expectedReviewEvent = new Event(DELETE, 1, null);
        MatcherAssert.assertThat(reviewMessages.get(0), Matchers.is(sameEventExceptCreatedAt(expectedReviewEvent)));
    }

    private void postAndVerifyProduct(ProductAggregate compositeProduct, HttpStatus expectedStatus) {
        client.post()
                .uri("/product-composite")
                .body(Mono.just(compositeProduct), ProductAggregate.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void purgeMessages(String bindingName) {
        getMessages(bindingName);
    }

    private List<String> getMessages(String bindingName){
        List<String> messages = new ArrayList<>();
        boolean anyMoreMessages = true;
        while (anyMoreMessages) {
            Message<byte[]> message = getMessage(bindingName);
            if (message == null) {
                anyMoreMessages = false;
            } else {
                messages.add(new String(message.getPayload()));
            }
        }
        return messages;
    }
    private Message<byte[]> getMessage(String bindingName){
        try {
            return target.receive(0, bindingName);
        } catch (NullPointerException npe) {
            log.error("getMessage() received a NPE with binding = {}", bindingName);
            return null;
        }
    }
    public static Matcher<String> sameEventExceptCreatedAt(Event expectedEvent) {
        return IsSameEvent.sameEventExceptCreatedAt(expectedEvent);
    }

    private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        client.delete()
                .uri("/product-composite/" + productId)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }
}
