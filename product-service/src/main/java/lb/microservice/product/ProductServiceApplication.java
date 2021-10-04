package lb.microservice.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan("lb.microservice")
public class ProductServiceApplication {

	public static void main(String[] args) {
		var context =SpringApplication.run(ProductServiceApplication.class, args);
		String mongoDbHost = context.getEnvironment().getProperty("spring.data.mongodb.host");
		String mongoDbPort = context.getEnvironment().getProperty("spring.data.mongodb.port");
		log.info("Connected to MongoDb: {}:{}", mongoDbHost, mongoDbPort);
	}

}
