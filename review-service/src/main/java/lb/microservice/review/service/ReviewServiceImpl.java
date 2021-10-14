package lb.microservice.review.service;

import lb.microservice.api.core.review.Review;
import lb.microservice.api.core.review.ReviewService;
import lb.microservice.api.exceptions.InvalidInputException;
import lb.microservice.review.percistence.ReviewEntity;
import lb.microservice.review.percistence.ReviewMapper;
import lb.microservice.review.percistence.ReviewRepository;
import lb.microservice.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ReviewServiceImpl implements ReviewService {
    private final ServiceUtil serviceUtil;
    private final ReviewMapper mapper;
    private final ReviewRepository repository;

    @Autowired
    public ReviewServiceImpl(ServiceUtil serviceUtil, ReviewMapper mapper, ReviewRepository repository) {
        this.serviceUtil = serviceUtil;
        this.mapper = mapper;
        this.repository = repository;
    }

    @Override
    public List<Review> getReviews(int productId) {

        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        List<ReviewEntity> reviewEntities = repository.findByProductId(productId);
        List<Review> reviews = reviewEntities.stream().map(mapper::entityToApi).toList();
        reviews.forEach(r -> r.setServiceAddress(serviceUtil.getServiceAddress()));
        log.debug("getReviews: response size: {}", reviews.size());

        return reviews;
    }

    @Override
    public Review createReview(Review review) {
        ReviewEntity entity = mapper.apiToEntity(review);
        try {
            ReviewEntity newEntity = repository.save(entity);
            log.debug("createReview: created a review entity: {}/{}", review.getProductId(), review.getReviewId());
            Review createdReview = mapper.entityToApi(newEntity);
            createdReview.setServiceAddress(serviceUtil.getServiceAddress());
            return createdReview;
        } catch (DataIntegrityViolationException e) {
            throw new InvalidInputException("Duplicate key, Product Id: " + review.getProductId() + ", Review Id:" + review.getReviewId());
        }
    }

    @Override
    public void deleteReviews(int productId) {
        log.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}
