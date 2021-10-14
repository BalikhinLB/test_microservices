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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public List<Recommendation> getRecommendations(int productId) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        List<Recommendation> recommendations = repository.findByProductId(productId).stream()
                .map(mapper::entityToApi)
                .toList();
        recommendations.forEach(r -> r.setServiceAddress(serviceUtil.getServiceAddress()));
        log.debug("getRecommendations: response size: {}", recommendations.size());
        return recommendations;
    }

    @Override
    public Recommendation createRecommendation(Recommendation recommendation) {
        RecommendationEntity entity = mapper.apiToEntity(recommendation);
        try {
            RecommendationEntity newEntity = repository.save(entity);
            log.debug("createRecommendation: created a recommendation entity: {}/{}", recommendation.getProductId(), recommendation.getRecommendationId());
            Recommendation created = mapper.entityToApi(newEntity);
            created.setServiceAddress(serviceUtil.getServiceAddress());
            return created;
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("Duplicate key, Product Id: " + recommendation.getProductId() + ", Recommendation Id:" + recommendation.getRecommendationId());
        }
    }

    @Override
    public void deleteRecommendations(int productId) {
        log.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}
