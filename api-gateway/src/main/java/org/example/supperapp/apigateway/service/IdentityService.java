package org.example.supperapp.apigateway.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.supperapp.apigateway.dto.request.IntrospectRequest;
import org.example.supperapp.apigateway.dto.response.ApiResponse;
import org.example.supperapp.apigateway.dto.response.IntrospectResponse;
import org.example.supperapp.apigateway.repository.httpClient.IdentityClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdentityService {
    IdentityClient identityClient;

    public Mono<ApiResponse<IntrospectResponse>> introspect(String token){
        return identityClient.introspect(IntrospectRequest.builder().token(token).build());
    }
}
