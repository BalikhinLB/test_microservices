package lb.microservice.api.composite.product;

public record ServiceAddresses(String productCompositeAddress, String productServiceAddress,
                               String recommendationServiceAddress, String reviewServiceAddress) {
}
