package lb.microservice.recommendation;

import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.recommendation.persistence.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecommendationServiceApplicationTests extends AbstractMongoDbTestBase {
    @Autowired
    private WebTestClient client;

    @Autowired
    private RecommendationRepository repository;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();
    }

    @Test
    void getRecommendationsByProductId() {

        int productId = 1;

        postAndVerifyRecommendation(productId, 1, HttpStatus.OK);
        postAndVerifyRecommendation(productId, 2, HttpStatus.OK);
        postAndVerifyRecommendation(productId, 3, HttpStatus.OK);

        assertEquals(3, repository.findByProductId(productId).size());

        getAndVerifyRecommendationsByProductId(productId, HttpStatus.OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].productId").isEqualTo(productId)
                .jsonPath("$[2].recommendationId").isEqualTo(3);
    }

    @Test
    void duplicateError() {

        int productId = 1;
        int recommendationId = 1;

        postAndVerifyRecommendation(productId, recommendationId, HttpStatus.OK)
                .jsonPath("$.productId").isEqualTo(productId)
                .jsonPath("$.recommendationId").isEqualTo(recommendationId);

        assertEquals(1, repository.count());

        postAndVerifyRecommendation(productId, recommendationId, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/recommendation")
                .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Recommendation Id:1");

        assertEquals(1, repository.count());
    }

	@Test
	void deleteRecommendations() {

		int productId = 1;
		int recommendationId = 1;

		postAndVerifyRecommendation(productId, recommendationId, HttpStatus.OK);
		assertEquals(1, repository.findByProductId(productId).size());

		deleteAndVerifyRecommendationsByProductId(productId, HttpStatus.OK);
		assertEquals(0, repository.findByProductId(productId).size());

		deleteAndVerifyRecommendationsByProductId(productId, HttpStatus.OK);
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

    private WebTestClient.BodyContentSpec postAndVerifyRecommendation(int productId, int recommendationId, HttpStatus expectedStatus) {
        Recommendation recommendation = new Recommendation(productId, recommendationId, "author " + recommendationId,
                recommendationId, "content " + recommendationId, "SA");
        return client.post()
                .uri("/recommendation")
                .body(just(recommendation), Recommendation.class)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
    }

    private WebTestClient.BodyContentSpec deleteAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
		return client.delete()
				.uri("/recommendation?productId=" + productId)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
    }

}
