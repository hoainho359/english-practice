package org.example.supperapp.apigateway.repository.httpClient;

import org.example.supperapp.apigateway.dto.request.IntrospectRequest;
import org.example.supperapp.apigateway.dto.response.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

public interface IdentityClient {
    // FIX: HttpServiceProxyFactory only creates HTTP interface methods from @HttpExchange annotations.
    // @PostMapping is a server-side MVC annotation and caused "Unexpected method invocation" at runtime.
    @PostExchange(
            url = "/auth/introspect",
            contentType = MediaType.APPLICATION_JSON_VALUE,
            accept = MediaType.APPLICATION_JSON_VALUE)
    Mono<ApiResponse<Boolean>> introspect(@RequestBody IntrospectRequest introspectRequest);
}
