package lb.microservice.recommendation;

import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.api.event.Event;
import lb.microservice.api.exceptions.InvalidInputException;
import lb.microservice.recommendation.persistence.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.function.Consumer;

import static lb.microservice.api.event.Event.Type.CREATE;
import static lb.microservice.api.event.Event.Type.DELETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false","spring.cloud.config.enabled=false"})
class RecommendationServiceApplicationTests extends AbstractMongoDbTestBase {
    @Autowired
    private WebTestClient client;

    @Autowired
    private RecommendationRepository repository;

    @Autowired
    @Qualifier("messageProcessor")
    private Consumer<Event<Integer, Recommendation>> messageProcessor;

    @BeforeEach
    void setupDb() {
        repository.deleteAll().block();
    }

    @Test
    void getRecommendationsByProductId() {

        int productId = 1;

        sendCreateRecommendationEvent(productId, 1);
        sendCreateRecommendationEvent(productId, 2);
        sendCreateRecommendationEvent(productId, 3);

        assertEquals(3, repository.findByProductId(productId).count().block());

        getAndVerifyRecommendationsByProductId(productId, HttpStatus.OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].productId").isEqualTo(productId)
                .jsonPath("$[2].recommendationId").isEqualTo(3);
    }

    @Test
    void duplicateError() {

        int productId = 1;
        int recommendationId = 1;

        sendCreateRecommendationEvent(productId, recommendationId);

        assertEquals(1, repository.count().block());

        assertThrows(InvalidInputException.class, () -> sendCreateRecommendationEvent(productId, recommendationId));

        assertEquals(1, repository.count().block());
    }

	@Test
	void deleteRecommendations() {

		int productId = 1;
		int recommendationId = 1;

        sendCreateRecommendationEvent(productId, recommendationId);
        assertEquals(1, repository.findByProductId(productId).count().block());

        sendDeleteRecommendationEvent(productId);
        assertEquals(0, repository.findByProductId(productId).count().block());

        sendDeleteRecommendationEvent(productId);
	}

    @Test
    void getRecommendationsInvalidParameter() {

        getAndVerifyRecommendationsByProductId("no-integer", HttpStatus.BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/recommendation")
                .jsonPath("$.error").isEqualTo("Bad Request");
    }

    @Test
    void getRecommendationsNotFound() {

        int productIdNotFound = 113;

        getAndVerifyRecommendationsByProductId(productIdNotFound, HttpStatus.OK)
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void getRecommendationsInvalidParameterNegativeValue() {

        int productIdInvalid = -1;

        getAndVerifyRecommendationsByProductId(productIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/recommendation")
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
        return getAndVerifyRecommendationsByProductId(String.valueOf(productId), expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productId, HttpStatus expectedStatus) {
        return client.get()
                .uri("/recommendation?productId=" + productId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void sendCreateRecommendationEvent(int productId, int recommendationId) {
        Recommendation recommendation = new Recommendation(productId, recommendationId, "Author " + recommendationId, recommendationId, "Content " + recommendationId, "SA");
        Event<Integer, Recommendation> event = new Event<>(CREATE, productId, recommendation);
        messageProcessor.accept(event);
    }

    private void sendDeleteRecommendationEvent(int productId) {
        Event<Integer, Recommendation> event = new Event<>(DELETE, productId, null);
        messageProcessor.accept(event);
    }

}
