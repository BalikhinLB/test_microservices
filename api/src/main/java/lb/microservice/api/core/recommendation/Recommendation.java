package lb.microservice.api.core.recommendation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class Recommendation {
    private int productId;
    private int recommendationId;
    private String author;
    private int rating;
    private String content;
    private String serviceAddress;

}
