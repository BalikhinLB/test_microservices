package lb.microservice.recommendation.service;

import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.api.core.recommendation.RecommendationService;
import lb.microservice.api.exceptions.InvalidInputException;
import lb.microservice.recommendation.persistence.RecommendationEntity;
import lb.microservice.recommendation.persistence.RecommendationMapper;
import lb.microservice.recommendation.persistence.RecommendationRepository;
import lb.microservice.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class RecommendationServiceImpl implements RecommendationService {
    private final ServiceUtil serviceUtil;

    private final RecommendationRepository repository;

    private final RecommendationMapper mapper;

    @Autowired
    public RecommendationServiceImpl(ServiceUtil serviceUtil, RecommendationRepository repository, RecommendationMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {
        checkProductId(productId);
        log.info("Will get recommendations for product with id={}", productId);
        return repository.findByProductId(productId)
                .log(log.getName(), FINE)
                .map(mapper::entityToApi)
                .map(this::setServiceAddress);
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation recommendation) {
        checkProductId(recommendation.getProductId());
        RecommendationEntity entity = mapper.apiToEntity(recommendation);
        log.debug("createRecommendation: will create a recommendation entity: {}/{}", recommendation.getProductId(), recommendation.getRecommendationId());
        return repository.save(entity)
                .log(log.getName(), FINE)
                .onErrorMap(
                        DuplicateKeyException.class,
                        ex -> new InvalidInputException("Duplicate key, Product Id: " + recommendation.getProductId() + ", Recommendation Id:" + recommendation.getRecommendationId()))
                .map(mapper::entityToApi);

    }

    @Override
    public Mono<Void> deleteRecommendations(int productId) {
        checkProductId(productId);

        log.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        return repository.deleteAll(repository.findByProductId(productId));
    }

    private void checkProductId(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
    }

    private Recommendation setServiceAddress(Recommendation recommendation) {
        recommendation.setServiceAddress(serviceUtil.getServiceAddress());
        return recommendation;
    }
}
