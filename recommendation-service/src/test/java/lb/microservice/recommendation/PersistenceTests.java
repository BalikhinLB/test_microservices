package lb.microservice.recommendation;

import lb.microservice.recommendation.persistence.RecommendationEntity;
import lb.microservice.recommendation.persistence.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class,
        properties = {"spring.cloud.config.enabled=false"})
class PersistenceTests extends AbstractMongoDbTestBase {
    @Autowired
    private RecommendationRepository repository;

    private RecommendationEntity savedEntity;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll().block();
        RecommendationEntity entity = new RecommendationEntity(1, 1, "author_1", 5, "recommendation_1");
        savedEntity = repository.save(entity).block();
        assertEqualsRecommendations(entity, savedEntity);
    }

    @Test
    void create() {
        var newEntity = new RecommendationEntity(2, 1, "author_2", 4, "recommendation_2");
        repository.save(newEntity).block();
        RecommendationEntity foundEntity = repository.findById(newEntity.getId()).block();
        assertEqualsRecommendations(newEntity, foundEntity);
        assertEquals(2, repository.count().block());
    }

    @Test
    void update() {
        savedEntity.setAuthor("author_3");
        repository.save(savedEntity).block();
        RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).block();
        assertEquals(1, foundEntity.getVersion());
        assertEquals("author_3", foundEntity.getAuthor());
    }

    @Test
    void delete() {
        repository.delete(savedEntity).block();
        assertFalse(repository.existsById(savedEntity.getId()).block());
    }

    @Test
    void getByProductId() {
        List<RecommendationEntity> entityList = repository.findByProductId(savedEntity.getProductId()).collectList().block();
        assertThat(entityList, hasSize(1));
        assertEqualsRecommendations(savedEntity, entityList.get(0));
    }

    @Test
    void duplicateError() {
        RecommendationEntity entity = new RecommendationEntity(savedEntity.getProductId(),
                savedEntity.getRecommendationId(), "author_4", 5, "recommendation_4");
        var saveMono = repository.save(entity);
        assertThrows(DuplicateKeyException.class, () -> saveMono.block());
    }
    @Test
    void optimisticLockError() {
        RecommendationEntity entity1 = repository.findById(savedEntity.getId()).block();
        RecommendationEntity entity2 = repository.findById(savedEntity.getId()).block();

        entity1.setAuthor("n1");
        repository.save(entity1).block();

        entity2.setAuthor("n2");
        var saveMono = repository.save(entity2);
        assertThrows(OptimisticLockingFailureException.class, () -> saveMono.block());

        RecommendationEntity updatedEntity = repository.findById(savedEntity.getId()).block();
        assertNotNull(updatedEntity);
        assertEquals(1, (int) updatedEntity.getVersion());
        assertEquals("n1", updatedEntity.getAuthor());
    }

    private void assertEqualsRecommendations(RecommendationEntity entity, RecommendationEntity savedEntity) {
        assertEquals(entity.getProductId(), savedEntity.getProductId());
        assertEquals(entity.getRecommendationId(), savedEntity.getRecommendationId());
        assertEquals(entity.getAuthor(), savedEntity.getAuthor());
        assertEquals(entity.getRating(), savedEntity.getRating());
        assertEquals(entity.getContent(), savedEntity.getContent());
    }
}
