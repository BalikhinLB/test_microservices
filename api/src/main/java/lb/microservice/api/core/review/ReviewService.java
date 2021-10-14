package lb.microservice.api.core.review;

import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface ReviewService {
    @GetMapping(value = "/review", produces = "application/json")
    List<Review> getReviews(@RequestParam int productId);


    @PostMapping(value = "/review", consumes = "application/json", produces = "application/json")
    Review createReview(@RequestBody Review review);

    @DeleteMapping(value = "/review")
    void deleteReviews(@RequestParam(value = "productId") int productId);
}
