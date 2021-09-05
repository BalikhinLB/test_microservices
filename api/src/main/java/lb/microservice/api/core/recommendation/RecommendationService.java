package lb.microservice.api.core.recommendation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface RecommendationService {
    @GetMapping(
            value = "/recommendation/{productId}",
            produces = "application/json")
    List<Recommendation> getRecommendation(@PathVariable int productId);
}
