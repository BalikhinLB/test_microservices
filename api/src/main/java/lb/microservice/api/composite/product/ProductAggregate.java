package lb.microservice.api.composite.product;

import lb.microservice.api.core.product.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProductAggregate {
    private int productId;
    private String name;
    private int weight;
    private List<RecommendationSummary> recommendations;
    private List<ReviewSummary> reviews;
    private ServiceAddresses serviceAddresses;

    public ProductAggregate(Product product, List<RecommendationSummary> recommendations, List<ReviewSummary> reviews, ServiceAddresses serviceAddresses) {
        this.productId = product.getProductId();
        this.name = product.getName();
        this.weight = product.getWeight();
        this.recommendations = recommendations;
        this.reviews = reviews;
        this.serviceAddresses = serviceAddresses;
    }
}
