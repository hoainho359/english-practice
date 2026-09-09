package org.example.supperapp.apigateway.configuaration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.example.supperapp.apigateway.dto.response.ApiResponse;
import org.example.supperapp.apigateway.service.IdentityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
@EnableFeignClients
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Configuration
public class AuthenticationFilter implements GlobalFilter, Ordered {
    IdentityService identityService;
    ObjectMapper objectMapper;

    @NonFinal
    String [] publicEndpoint = {"/identity/auth/login", "/identity/users/registration"};
    @Value("${app.api-prefix}")
    @NonFinal
    String apiPrefix;
    @NonFinal
    AntPathMatcher pathMatcher = new AntPathMatcher();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Allow CORS preflight requests through without authentication
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        //check public endpoint
        if (isPublicEndpoint(exchange.getRequest())){
            return chain.filter(exchange);
        }
        //check token user request
        List<String> headers = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);

        if (CollectionUtils.isEmpty(headers)){
            return unAuthentication(exchange.getResponse());
        }

        //if token exists and verify
        String bearer = headers.getFirst().replace("Bearer ", "");

        /*
        flatMap có nhiệm vụ làm phẳng cấu trúc Mono<Mono<Void>> đó thành một Mono<Void> duy nhất,
         giúp chuỗi xử lý (reactive chain) chạy mượt mà theo đúng chuẩn của WebFlux.
         */
        return identityService.introspect(bearer).flatMap(introspectResponseApiResponse ->
        {
            if (introspectResponseApiResponse.getResult().isValid()){
                return chain.filter(exchange);
            }
            else
                return unAuthentication(exchange.getResponse());

        }).onErrorResume(throwable -> unAuthentication(exchange.getResponse()));
    }

    public Mono<Void> unAuthentication(ServerHttpResponse serverHttpResponse){
        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(1401)
                .message("unauthentication")
                .build();
        String body = null;

        body = objectMapper.writeValueAsString(apiResponse);

        serverHttpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
        serverHttpResponse.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        return serverHttpResponse.writeWith(
                Mono.just(serverHttpResponse.bufferFactory().wrap(body.getBytes()))
        );
    }
    public boolean isPublicEndpoint(ServerHttpRequest request){
        log.info("rq path{}: ", request.getURI().getPath());
//        rq path/api/identity/auth/token
//        tức là nó chir trả về endpoint sau phần context path mình khai báo ở
//        applicaiton.yaml hay properties
        // Dùng pathMatcher.match thay cho matches() của String
        boolean isPublic = Arrays.stream(publicEndpoint)
                .anyMatch(s -> pathMatcher.match(apiPrefix + s, request.getURI().getPath()));

        log.info("isPublic = {}", isPublic);

        return isPublic;
    }
    @Override
    public int getOrder() {
        return -1;
    }
}
