package lb.microservice.product;

import lb.microservice.product.persistence.ProductEntity;
import lb.microservice.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class,
        properties = {"spring.cloud.config.enabled=false"})
class PersistenceTests extends AbstractMongoDbTestBase {
    @Autowired
    private ProductRepository repository;

    private ProductEntity savedEntity;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll().block();
        ProductEntity entity = new ProductEntity(1, "n", 2);
        StepVerifier.create(repository.save(entity))
                .expectNextMatches(createdEntity -> {
                    savedEntity = createdEntity;
                    return assertEqualsProduct(savedEntity, createdEntity);
                })
                .verifyComplete();
    }

    @Test
    void create() {
        ProductEntity newEntity = new ProductEntity(2, "n", 2);
        StepVerifier.create(repository.save(newEntity))
                .expectNextMatches(createdEntity -> assertEqualsProduct(newEntity, createdEntity))
                .verifyComplete();

        StepVerifier.create(repository.findById(newEntity.getId()))
                .expectNextMatches(foundEntity -> assertEqualsProduct(newEntity, foundEntity))
                .verifyComplete();
        StepVerifier.create(repository.count())
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void update() {
        savedEntity.setName("n2");
        StepVerifier.create(repository.save(savedEntity))
                .expectNextMatches(updatedEntity -> "n2".equals(updatedEntity.getName()))
                .verifyComplete();
        StepVerifier.create(repository.findById(savedEntity.getId()))
                .expectNextMatches(fundEntity -> 1 == fundEntity.getVersion() && "n2".equals(fundEntity.getName()))
                .verifyComplete();
    }

    @Test
    void delete() {
        StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
        StepVerifier.create(repository.existsById(savedEntity.getId()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void getByProductId() {
        StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
                .expectNextMatches(entity -> assertEqualsProduct(savedEntity, entity))
                .verifyComplete();
    }

    @Test
    void duplicateError() {
        ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
        StepVerifier.create(repository.save(entity))
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    void optimisticLockError() {
        ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
        ProductEntity entity2 = repository.findById(savedEntity.getId()).block();

        entity1.setName("n1");
        repository.save(entity1).block();

        entity2.setName("n2");
        StepVerifier.create(repository.save(entity2))
                .expectError(OptimisticLockingFailureException.class)
                .verify();

        StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
                .expectNextMatches(updatedEntity -> 1 == updatedEntity.getVersion() && "n1".equals(updatedEntity.getName()))
                .verifyComplete();

    }

    private boolean assertEqualsProduct(ProductEntity product, ProductEntity savedProduct) {
        assertEquals(product.getProductId(), savedProduct.getProductId());
        assertEquals(product.getName(), savedProduct.getName());
        assertEquals(product.getWeight(), savedProduct.getWeight());
        return true;
    }
}
