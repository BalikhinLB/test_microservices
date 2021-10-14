package lb.microservice.product.composite.service;

import lb.microservice.api.composite.product.*;
import lb.microservice.api.core.product.Product;
import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.api.core.review.Review;
import lb.microservice.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

    private final ServiceUtil serviceUtil;
    private final ProductCompositeIntegration integration;

    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public ProductAggregate getProduct(int productId) {
        var product = integration.getProduct(productId);
        var recommendations = integration.getRecommendations(productId);
        var reviews = integration.getReviews(productId);
        return createProductAggregate(product, recommendations, reviews, serviceUtil.getServiceAddress());
    }

    @Override
    public void createProduct(ProductAggregate body) {
        try {
            Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            integration.createProduct(product);
            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    Recommendation recommendation = new Recommendation(body.getProductId(),
                            r.getRecommendationId(), r.getAuthor(), r.getRate(),
                            r.getContent(), null);
                    integration.createRecommendation(recommendation);
                });
            }
            if (body.getReviews() != null) {
                body.getReviews().forEach(r -> {
                    Review review = new Review(body.getProductId(),
                            r.getReviewId(), r.getAuthor(), r.getSubject(),
                            r.getContent(), null);
                    integration.createReview(review);
                });
            }
        } catch (RuntimeException re) {
            log.warn("createCompositeProduct failed", re);
            throw re;
        }
    }

    @Override
    public void deleteProduct(int productId) {
        integration.deleteProduct(productId);
        integration.deleteRecommendations(productId);
        integration.deleteReviews(productId);
    }

    private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews, String productCompositeAddress) {
        List<RecommendationSummary> recommendationSummaries = recommendations == null ? null : recommendations.stream()
                .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRating(), r.getContent()))
                .toList();
        List<ReviewSummary> reviewSummaries = reviews == null ? null : reviews.stream()
                .map(r -> new ReviewSummary(r.getReviewId(), r.getSubject(), r.getAuthor(), r.getContent()))
                .toList();
        String productAddress = product.getServiceAddress();
        String recommendationAddress = CollectionUtils.isEmpty(recommendations) ? "" : recommendations.get(0).getServiceAddress();
        String reviewAddress = CollectionUtils.isEmpty(reviews) ? "" : reviews.get(0).getServiceAddress();
        var serviceAddress = new ServiceAddresses(productCompositeAddress, productAddress, recommendationAddress, reviewAddress);
        return new ProductAggregate(product, recommendationSummaries, reviewSummaries, serviceAddress);
    }
}
