package com.liondevs.apigateway.config;

import com.liondevs.apigateway.config.dto.ValidationTokenDto;
import com.liondevs.apigateway.config.exception.DefaultException;
import com.liondevs.apigateway.config.exception.InvalidJwtException;
import com.liondevs.apigateway.config.exception.MissingTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;

//custom filter to validate auth
@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {
   private final WebClient.Builder webClientBuilder;

    private static final CharSequence[] AUTH_WHITELIST = {
            "/api/v1/auth/",
            "/error",
            // Actuators
            "/actuator/",
            "/health/"
    };

    private CharSequence canContinue( URI uri){

        for (CharSequence path : AUTH_WHITELIST) {
            if (uri.toString().contains(path)) {
                return path;
            }
        }
        return null;
    }

    public AuthFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
       this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
        try {
            ServerHttpRequest request = exchange.getRequest();
            URI uri = request.getURI();
            CharSequence path =    canContinue(uri);
            if(path != null){
                return chain.filter(exchange);
            }
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
               throw  new MissingTokenException("Missing authorization Header", HttpStatus.BAD_REQUEST);
            }

            String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);

            String[] parts = authHeader.split(" ");

            if (parts.length != 2 || !"Bearer".equals(parts[0])) {
                throw  new InvalidJwtException("invalid token", HttpStatus.BAD_REQUEST);
            }
            return webClientBuilder.build()
                    .post()
                    .uri("http://fast-food-app-auth-server/api/v1/auth/validate-token?token=" + parts[1])
                    .retrieve().bodyToMono(ValidationTokenDto.class)
                    .map(tokenDto -> {
                    log.info("------TokenDto----- : ${}",tokenDto);
                        try {
                            if(!Objects.equals(tokenDto.getCode(), "200")) throw  new InvalidJwtException("Invalid Jwt Token", HttpStatus.BAD_REQUEST);
                            exchange.getRequest()
                                    .mutate()
                                    .header("X-Auth-User-id", String.valueOf(tokenDto.getIdUser()),"X-Auth-User-Email", String.valueOf(tokenDto.getEmail()) );
                            return exchange;
                        }catch (InvalidJwtException e){
                        throw  new ResponseStatusException( HttpStatus.BAD_REQUEST,e.getMessage());
                        }
                    }).flatMap(chain::filter);
        }catch (DefaultException e){
            return Mono.error(new ResponseStatusException( HttpStatus.INTERNAL_SERVER_ERROR,e.getMessage()));
        }catch (MissingTokenException e){
            return Mono.error(new ResponseStatusException( HttpStatus.BAD_REQUEST,e.getMessage()));
        }catch (InvalidJwtException e){
            return Mono.error(new ResponseStatusException( HttpStatus.BAD_REQUEST,e.getMessage()));
        }
        };
    }

    public static class Config {
        // empty class as I don't need any particular configuration
    }
}
