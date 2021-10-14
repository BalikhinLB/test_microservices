package lb.microservice.product.composite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lb.microservice.api.core.product.Product;
import lb.microservice.api.core.product.ProductService;
import lb.microservice.api.core.recommendation.Recommendation;
import lb.microservice.api.core.recommendation.RecommendationService;
import lb.microservice.api.core.review.Review;
import lb.microservice.api.core.review.ReviewService;
import lb.microservice.api.exceptions.InvalidInputException;
import lb.microservice.api.exceptions.NotFoundException;
import lb.microservice.util.HttpErrorInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private static final String URL_TEMPLATE = "http://%s:%s/%s";

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    @Autowired
    public ProductCompositeIntegration(RestTemplate restTemplate, ObjectMapper mapper,
                                       @Value("${app.product-service.host}")
                                       String productServiceHost,
                                       @Value("${app.product-service.port}")
                                       String productServicePort,
                                       @Value("${app.recommendation-service.host}")
                                       String recommendationServiceHost,
                                       @Value("${app.recommendation-service.port}")
                                       String recommendationServicePort,
                                       @Value("${app.review-service.host}")
                                       String reviewServiceHost,
                                       @Value("${app.review-service.port}")
                                       String reviewServicePort) {

        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.productServiceUrl = String.format(URL_TEMPLATE, productServiceHost, productServicePort, "product");
        this.recommendationServiceUrl = String.format(URL_TEMPLATE, recommendationServiceHost, recommendationServicePort, "recommendation?productId=");
        this.reviewServiceUrl = String.format(URL_TEMPLATE, reviewServiceHost, reviewServicePort, "review?productId=");
    }

    @Override
    public Product getProduct(int productId) {
        try {
            String url = productServiceUrl + "/" + productId;
            log.debug("Will call getProduct API by URL:{}", url);
            var product = restTemplate.getForObject(url, Product.class);
            if (product == null) {
                log.warn("Not found product with id:{}", productId);
            } else {
                log.debug("Found product with id:{}", product.getProductId());
            }
            return product;
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public Product createProduct(Product body) {
        try {
            String url = productServiceUrl;
            log.debug("Will post a new product to URL: {}", url);
            var product = restTemplate.postForObject(url, body, Product.class);
            log.debug("Created a product with id: {}", body.getProductId());
            return product;
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public void deleteProduct(int productId) {
        try {
            String url = productServiceUrl + "/" + productId;
            log.debug("Will call deleteProduct API on url:{}", url);
            restTemplate.delete(url);
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {
        try {
            String url = recommendationServiceUrl + productId;
            log.debug("Will call getRecommendations API by URL:{}", url);
            List<Recommendation> recommendations = restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Recommendation>>() {})
                    .getBody();
            if (recommendations == null) {
                log.debug("Not found any recommendation for productId {}", productId);
            } else {
                log.debug("Found {} recommendations for productId {}", recommendations.size(), productId);
            }
            return recommendations;
        } catch (Exception ex) {
            log.warn("Got an exception while requesting recommendations, return zero recommendations: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        try {
            String url = recommendationServiceUrl;
            log.debug("Will post a new recommendation to URL: {}", url);

            Recommendation recommendation = restTemplate.postForObject(url, body, Recommendation.class);
            log.debug("Created a recommendation with id: {}", body.getProductId());

            return recommendation;
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public void deleteRecommendations(int productId) {
        try {
            String url = recommendationServiceUrl + productId;
            log.debug("Will call deleteRecommendations API on URL: {}", url);
            restTemplate.delete(url);
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public List<Review> getReviews(int productId) {
        try {
            String url = reviewServiceUrl + productId;
            log.debug("Will call getReviews API by URL:{}", url);
            List<Review> reviews = restTemplate.exchange(url, HttpMethod.GET, null,
                            new ParameterizedTypeReference<List<Review>>() {})
                            .getBody();
            if (reviews == null) {
                log.debug("Not found any reviews for productId {}", productId);
            } else {
                log.debug("Found {} reviews for productId {}", reviews.size(), productId);
            }
            return reviews;
        } catch (Exception ex) {
            log.warn("Got an exception while requesting reviews, return zero recommendations: {}", ex.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public Review createReview(Review body) {
        try {
            String url = reviewServiceUrl + body.getProductId();
            log.debug("Will post a new review to URL: {}", url);

            Review recommendation = restTemplate.postForObject(url, body, Review.class);
            log.debug("Created a recommendation with id: {}", body.getProductId());

            return recommendation;
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public void deleteReviews(int productId) {
        try {
            String url = reviewServiceUrl + productId;
            log.debug("Will call deleteReviews API on URL: {}", url);
            restTemplate.delete(url);
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    private RuntimeException handleHttpClientException(HttpClientErrorException ex) {
        return switch (ex.getStatusCode()) {
            case NOT_FOUND -> new NotFoundException(getErrorMessage(ex));
            case UNPROCESSABLE_ENTITY -> new InvalidInputException(getErrorMessage(ex));
            default -> {
                log.warn("Got an unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                log.warn("Error body: {}", ex.getResponseBodyAsString());
                yield ex;
            }
        };
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }
}
