package com.acme.employee.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.server.SecurityWebFilterChain;
// Removed custom AuthenticationWebFilter wiring to avoid version conflicts.
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import java.util.Objects;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Allow CORS preflight requests
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public GET endpoints
                        .pathMatchers(HttpMethod.GET, "/api/schema", "/api/schema/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/employees", "/api/employees/**").permitAll()
                        .pathMatchers("/actuator/**", "/api/docs", "/api/swagger-ui/**").permitAll()
                        // Auth debug endpoint requires authentication
                        .pathMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        // Protect writes
                        .pathMatchers(HttpMethod.POST, "/api/**").authenticated()
                        .pathMatchers(HttpMethod.PUT, "/api/**").authenticated()
                        .pathMatchers(HttpMethod.PATCH, "/api/**").authenticated()
                        .pathMatchers(HttpMethod.DELETE, "/api/**").authenticated()
                        // Everything else permitted (adjust as needed)
                        .anyExchange().permitAll()
                )
                .build();
    }

    // Lightweight WebFilter that verifies Firebase ID token (if present) and sets Authentication in context.
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter firebaseAuthWebFilter() {
        return (exchange, chain) -> extractBearer(exchange)
                .flatMap(this::verifyIdToken)
                .flatMap(uid -> {
                    Authentication auth = new UsernamePasswordAuthenticationToken(uid, "N/A", AuthorityUtils.NO_AUTHORITIES);
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<String> extractBearer(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return Mono.just(auth.substring(7));
        }
        return Mono.empty();
    }

    private Mono<String> verifyIdToken(String token) {
        return Mono.fromCallable(() -> {
                    try {
                        FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
                        return decoded.getUid();
                    } catch (FirebaseAuthException e) {
                        log.warn("Firebase token verification failed: {}", e.getMessage());
                        return null;
                    } catch (IllegalStateException e) {
                        // Firebase Admin not initialized yet
                        log.warn("Firebase Admin not initialized: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }
}
