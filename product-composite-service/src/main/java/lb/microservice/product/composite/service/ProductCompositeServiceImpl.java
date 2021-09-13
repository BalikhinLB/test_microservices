package lb.microservice.product.composite.service;

import lb.microservice.api.composite.product.*;
import lb.microservice.api.core.product.Product;
import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.api.core.review.Review;
import lb.microservice.util.ServiceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews, String productCompositeAddress) {
        List<RecommendationSummary> recommendationSummaries = recommendations == null ? null : recommendations.stream()
                .map(r -> new RecommendationSummary(r.recommendationId(),
                        r.author(), r.rate(), r.content()))
                .toList();
        List<ReviewSummary> reviewSummaries = reviews == null ? null : reviews.stream()
                .map(r -> new ReviewSummary(r.reviewId(), r.subject(), r.author(), r.content()))
                .toList();
        String productAddress = product.serviceAddress();
        String recommendationAddress = CollectionUtils.isEmpty(recommendations) ? "" : recommendations.get(0).serviceAddress();
        String reviewAddress = CollectionUtils.isEmpty(reviews) ? "" : reviews.get(0).serviceAddress();
        var serviceAddress = new ServiceAddresses(productCompositeAddress, productAddress, recommendationAddress, reviewAddress);
        return new ProductAggregate(product, recommendationSummaries, reviewSummaries, serviceAddress);
    }
}
