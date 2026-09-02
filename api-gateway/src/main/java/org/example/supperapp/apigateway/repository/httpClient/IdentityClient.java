package org.example.supperapp.apigateway.repository.httpClient;

import org.example.supperapp.apigateway.dto.request.IntrospectRequest;
import org.example.supperapp.apigateway.dto.response.ApiResponse;
import org.example.supperapp.apigateway.dto.response.IntrospectResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

@FeignClient(url = "http://localhost:8080/identity", name = "identity-client")
public interface IdentityClient {
    @PostMapping(value = "/auth/introspect", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<ApiResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest introspectRequest);
}
