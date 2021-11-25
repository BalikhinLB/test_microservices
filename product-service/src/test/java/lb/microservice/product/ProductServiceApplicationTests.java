package lb.microservice.product;

import lb.microservice.api.core.product.Product;
import lb.microservice.api.event.Event;
import lb.microservice.api.exceptions.InvalidInputException;
import lb.microservice.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

import static lb.microservice.api.event.Event.Type.CREATE;
import static lb.microservice.api.event.Event.Type.DELETE;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"eureka.client.enabled=false","spring.cloud.config.enabled=false"})
class ProductServiceApplicationTests extends AbstractMongoDbTestBase {

    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductRepository repository;

    @Autowired
    @Qualifier("messageProcessor")
    private Consumer<Event<Integer, Product>> messageProcessor;

    @BeforeEach
    void setupDb() {
        repository.deleteAll().block();
    }

    @Test
    void getProductById() {

        int productId = 1;
        assertNull(repository.findByProductId(productId).block());

        sendCreateProductEvent(productId);
        assertNotNull(repository.findByProductId(productId).block());

        getAndVerifyProduct(productId, HttpStatus.OK)
                .jsonPath("$.productId").isEqualTo(productId);

    }

    @Test
    void getProductInvalidParameterString() {

        getAndVerifyProduct("no-integer", HttpStatus.BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/product/no-integer")
                .jsonPath("$.error").isEqualTo("Bad Request");

    }

    @Test
    void getProductNotFound() {

        int productIdNotFound = 13;

        getAndVerifyProduct(productIdNotFound, HttpStatus.NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
                .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
    }

    @Test
    void getProductInvalidParameterNegativeValue() {

        int productIdInvalid = -1;

        getAndVerifyProduct(productIdInvalid, HttpStatus.UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    @Test
    void duplicateError() {
        int productId = 1;
        assertNull(repository.findByProductId(productId).block());
        sendCreateProductEvent(productId);
        assertNotNull(repository.findByProductId(productId).block());
        StepVerifier.create(repository.findByProductId(productId))
                .expectNextMatches(productEntity -> productId == productEntity.getProductId())
                .verifyComplete();
        assertThrows(InvalidInputException.class, () -> sendCreateProductEvent(productId));
    }

    @Test
    void deleteProduct() {
        int productId = 1;
        sendCreateProductEvent(productId);
        assertNotNull(repository.findByProductId(productId).block());

        sendDeleteProductEvent(productId);

        assertNull(repository.findByProductId(productId).block());
        sendDeleteProductEvent(productId);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        return getAndVerifyProduct(String.valueOf(productId), expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(String productId, HttpStatus expectedStatus) {
        return client.get()
                .uri("/product/" + productId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void sendCreateProductEvent(int productId) {
        Product product = new Product(productId, "Name " + productId, productId, "SA");
        Event<Integer, Product> event = new Event<>(CREATE, productId, product);
        messageProcessor.accept(event);
    }

    private void sendDeleteProductEvent(int productId) {
        Event<Integer, Product> event = new Event<>(DELETE, productId, null);
        messageProcessor.accept(event);
    }

}
