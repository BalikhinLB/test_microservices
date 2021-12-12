package lb.microservice.product.service;

import lb.microservice.api.core.product.Product;
import lb.microservice.api.core.product.ProductService;
import lb.microservice.api.exceptions.InvalidInputException;
import lb.microservice.api.exceptions.NotFoundException;
import lb.microservice.product.persistence.ProductEntity;
import lb.microservice.product.persistence.ProductMapper;
import lb.microservice.product.persistence.ProductRepository;
import lb.microservice.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;
import java.util.logging.Level;

@Slf4j
@RestController
public class ProductServiceImpl implements ProductService {

    private final Random randomNumberGenerator = new Random();
    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil, ProductRepository repository, ProductMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Product> getProduct(int productId, int delay, int faultPercent) {
        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }
        return repository.findByProductId(productId)
                .map(e -> throwErrorIfBadLuck(e, faultPercent))
                .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
                .log(log.getName(), Level.FINE)
                .map(mapper::entityToApi)
                .delayElement(Duration.ofSeconds(delay))
                .map(this::setServiceAddress);
    }

    @Override
    public Mono<Product> createProduct(Product body) {
        ProductEntity entity = mapper.apiToEntity(body);
        return repository.save(entity)
                .log(log.getName(), Level.FINE)
                .onErrorMap(DuplicateKeyException.class, ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
                .map(mapper::entityToApi)
                .map(this::setServiceAddress);
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        log.debug("deleteProduct: tries to delete product with productId: {}", productId);
        return repository.findByProductId(productId)
                .log(log.getName(), Level.FINE)
                .map(repository::delete)
                .flatMap(e -> e);
    }

    private Product setServiceAddress(Product product) {
        product.setServiceAddress(serviceUtil.getServiceAddress());
        return product;
    }

    private ProductEntity throwErrorIfBadLuck(ProductEntity entity, int faultPercent) {
        if (faultPercent == 0) {
            return entity;
        }
        int randomThreshold = getRandomNumber(1, 100);
        if (faultPercent < randomThreshold) {
            log.debug("We got lucky, no error occurred, {} < {}", faultPercent, randomThreshold);
        } else {
            log.debug("Bad luck, an error occurred, {} >= {}", faultPercent, randomThreshold);
            throw new RuntimeException("Something went wrong...");
        }
        return entity;
    }

    private int getRandomNumber(int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("Max must be greater than min");
        }
        return randomNumberGenerator.nextInt((max - min) + 1) + min;
    }
}
