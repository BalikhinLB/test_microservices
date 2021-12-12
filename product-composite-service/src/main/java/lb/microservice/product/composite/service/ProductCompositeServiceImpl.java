package lb.microservice.product.composite.service;

import lb.microservice.api.composite.product.*;
import lb.microservice.api.core.product.Product;
import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.api.core.review.Review;
import lb.microservice.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URL;
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
    public Mono<ProductAggregate> getProduct(int productId, int delay, int faultPercent) {
        return Mono.zip(values -> createProductAggregate((SecurityContext) values[0],
                                (Product) values[1],
                                (List<Recommendation>) values[2],
                                (List<Review>) values[3],
                                serviceUtil.getServiceAddress()),
                        getSecurityContextMono(),
                        integration.getProduct(productId, delay, faultPercent),
                        integration.getRecommendations(productId).collectList(),
                        integration.getReviews(productId).collectList())
                .doOnError(ex -> log.warn("getCompositeProduct filed: {}", ex.toString()))
                .log(log.getName(), Level.FINE);
    }

    @Override
    public Mono<Void> createProduct(ProductAggregate body) {
        try {
            List<Mono<?>> monoList = new ArrayList<>();

            monoList.add(getLogAuthorizationInfoMono());

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
                            getLogAuthorizationInfoMono(),
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

    private ProductAggregate createProductAggregate(SecurityContext sc, Product product, List<Recommendation> recommendations,
                                                    List<Review> reviews, String productCompositeAddress) {
        logAuthorizationInfo(sc);
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

    private Mono<SecurityContext> getLogAuthorizationInfoMono() {
        return getSecurityContextMono().doOnNext(this::logAuthorizationInfo);
    }

    private Mono<SecurityContext> getSecurityContextMono() {
        return ReactiveSecurityContextHolder.getContext().defaultIfEmpty(new SecurityContextImpl());
    }

    private void logAuthorizationInfo(SecurityContext sc) {
        if (sc != null && sc.getAuthentication() != null && sc.getAuthentication() instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            logAuthorizationInfo(jwtAuthenticationToken.getToken());
        } else {
            log.warn("No JWT based Authentication supplied");
        }
    }

    private void logAuthorizationInfo(Jwt jwt) {
        if (jwt == null) {
            log.warn("No JWT supplied");
        } else {
            if (log.isDebugEnabled()) {
                URL issuer = jwt.getIssuer();
                List<String> audience = jwt.getAudience();
                Object subject = jwt.getClaims().get("sub");
                Object scopes = jwt.getClaims().get("scope");
                Object expires = jwt.getClaims().get("exp");

                log.debug("Authorization info: Subject: {}, scopes: {}, expires {}: issuer: {}, audience: {}", subject, scopes, expires, issuer, audience);
            }
        }
    }
}
