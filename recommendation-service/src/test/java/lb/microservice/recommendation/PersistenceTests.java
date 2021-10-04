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

@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
class PersistenceTests extends AbstractMongoDbTestBase {
    @Autowired
    private RecommendationRepository repository;

    private RecommendationEntity savedEntity;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll();
        RecommendationEntity entity = new RecommendationEntity(1, 1, "author_1", 5, "recommendation_1");
        savedEntity = repository.save(entity);
        assertEqualsRecommendations(entity, savedEntity);
    }

    @Test
    void create() {
        var newEntity = new RecommendationEntity(2, 1, "author_2", 4, "recommendation_2");
        repository.save(newEntity);
        RecommendationEntity foundEntity = repository.findById(newEntity.getId()).get();
        assertEqualsRecommendations(newEntity, foundEntity);
        assertEquals(2, repository.count());
    }

    @Test
    void update() {
        savedEntity.setAuthor("author_3");
        repository.save(savedEntity);
        RecommendationEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, foundEntity.getVersion());
        assertEquals("author_3", foundEntity.getAuthor());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    void getByProductId() {
        List<RecommendationEntity> entityList = repository.findByProductId(savedEntity.getProductId());
        assertThat(entityList, hasSize(1));
        assertEqualsRecommendations(savedEntity, entityList.get(0));
    }

    @Test
    void duplicateError() {
        RecommendationEntity entity = new RecommendationEntity(savedEntity.getProductId(),
                savedEntity.getRecommendationId(), "author_4", 5, "recommendation_4");
        assertThrows(DuplicateKeyException.class, () -> repository.save(entity));
    }
    @Test
    void optimisticLockError() {
        RecommendationEntity entity1 = repository.findById(savedEntity.getId()).get();
        RecommendationEntity entity2 = repository.findById(savedEntity.getId()).get();

        entity1.setAuthor("n1");
        repository.save(entity1);

        entity2.setAuthor("n2");
        assertThrows(OptimisticLockingFailureException.class, () -> repository.save(entity2));

        RecommendationEntity updatedEntity = repository.findById(savedEntity.getId()).get();
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
