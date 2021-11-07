package lb.microservice.product.composite;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http.authorizeExchange()
			.pathMatchers("/actuator/**").permitAll()
			.anyExchange().authenticated()
			.and()
			.oauth2ResourceServer().jwt();
		return http.build();
	}

}
