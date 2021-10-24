package lb.microservice.api.core.recommendation;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecommendationService {
    @GetMapping(value = "/recommendation", produces = "application/json")
    Flux<Recommendation> getRecommendations(@RequestParam(value = "productId") int productId);

    @PostMapping(value = "/recommendation", consumes = "application/json", produces = "application/json")
    Mono<Recommendation> createRecommendation(@RequestBody Recommendation recommendation);

    @DeleteMapping(value = "/recommendation")
    Mono<Void> deleteRecommendations(@RequestParam(value = "productId") int productId);
}
