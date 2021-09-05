package lb.microservice.api.core.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface ReviewService {
    @GetMapping(
            value = "/review/{productId}",
            produces = "application/json")
    List<Review> getReviews(@PathVariable int productId);
}
