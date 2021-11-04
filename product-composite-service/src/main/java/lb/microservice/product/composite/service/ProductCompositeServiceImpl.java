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
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

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

    @SuppressWarnings("unchecked")
	@Override
    public Mono<ProductAggregate> getProduct(int productId) {
        return Mono.zip(values -> createProductAggregate((Product) values[0], (List<Recommendation>) values[1], (List<Review>) values[2], serviceUtil.getServiceAddress()),
                        integration.getProduct(productId), integration.getRecommendations(productId).collectList(), integration.getReviews(productId).collectList())
                .doOnError(ex -> log.warn("getCompositeProduct filed: {}", ex.toString()))
                .log(log.getName(), Level.FINE);
    }

    @Override
    public Mono<Void> createProduct(ProductAggregate body) {
        try {
            List<Mono<?>> monoList = new ArrayList<>();

            log.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

            Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            monoList.add(integration.createProduct(product));

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
                    monoList.add(integration.createRecommendation(recommendation));
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach(r -> {
                    Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
                    monoList.add(integration.createReview(review));
                });
            }

            log.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());
            return Mono.zip(r -> "", monoList.toArray(new Mono[0]))
                    .doOnError(ex -> log.warn("createCompositeProduct failed: {}", ex.toString()))
                    .then();

        } catch (RuntimeException re) {
            log.warn("createCompositeProduct failed: {}", re.toString());
            throw re;
        }
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        try {

            log.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

            return Mono.zip(r -> "",
                            integration.deleteProduct(productId),
                            integration.deleteRecommendations(productId),
                            integration.deleteReviews(productId))
                    .doOnError(ex -> log.warn("delete failed: {}", ex.toString()))
                    .log(log.getName(), Level.FINE).then();

        } catch (RuntimeException re) {
            log.warn("deleteCompositeProduct failed: {}", re.toString());
            throw re;
        }
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
