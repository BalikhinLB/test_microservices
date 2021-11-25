package lb.microservice.review;

import lb.microservice.review.percistence.ReviewEntity;
import lb.microservice.review.percistence.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@DataJpaTest(properties = {"spring.cloud.config.enabled=false"})
@Transactional(propagation = NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceTests extends AbstractMySqlTestBase{

    @Autowired
    private ReviewRepository repository;

    private ReviewEntity savedEntity;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll();
        ReviewEntity entity = new ReviewEntity(1, 1, "author_1", "subject_1", "review_1");
        savedEntity = repository.save(entity);
        assertEqualsReview(entity, savedEntity);
    }

    @Test
    void create() {
        var newEntity = new ReviewEntity(1, 2, "author_2", "subject_2", "review_2");
        repository.save(newEntity);
        ReviewEntity foundEntity = repository.findById(newEntity.getId()).get();
        assertEqualsReview(newEntity, foundEntity);
        assertEquals(2, repository.count());
    }

    @Test
    void update() {
        savedEntity.setAuthor("author_3");
        repository.save(savedEntity);
        ReviewEntity foundEntity = repository.findById(savedEntity.getId()).get();
        assertEquals("author_3", foundEntity.getAuthor());
        assertEquals(1, foundEntity.getVersion());
    }

    @Test
    void delete() {
        repository.delete(savedEntity);
        assertFalse(repository.existsById(savedEntity.getId()));
    }

    @Test
    void getByProductId() {
        List<ReviewEntity> entityList = repository.findByProductId(savedEntity.getProductId());
        assertThat(entityList, hasSize(1));
        assertEqualsReview(savedEntity, entityList.get(0));
    }

    @Test
    void duplicateError() {
        ReviewEntity entity = new ReviewEntity(savedEntity.getProductId(),
                savedEntity.getReviewId(), "author_4", "subject_4", "review_4");
        assertThrows(DataIntegrityViolationException.class, () -> repository.save(entity));
    }
    @Test
    void optimisticLockError() {
        ReviewEntity entity1 = repository.findById(savedEntity.getId()).get();
        ReviewEntity entity2 = repository.findById(savedEntity.getId()).get();

        entity1.setAuthor("n1");
        repository.save(entity1);

        entity2.setAuthor("n2");
        assertThrows(OptimisticLockingFailureException.class, () -> repository.save(entity2));

        ReviewEntity updatedEntity = repository.findById(savedEntity.getId()).get();
        assertEquals(1, updatedEntity.getVersion());
        assertEquals("n1", updatedEntity.getAuthor());
    }

    private void assertEqualsReview(ReviewEntity entity, ReviewEntity savedEntity) {
        assertEquals(entity.getProductId(), savedEntity.getProductId());
        assertEquals(entity.getReviewId(), savedEntity.getReviewId());
        assertEquals(entity.getAuthor(), savedEntity.getAuthor());
        assertEquals(entity.getSubject(), savedEntity.getSubject());
        assertEquals(entity.getContent(), savedEntity.getContent());
    }

}
