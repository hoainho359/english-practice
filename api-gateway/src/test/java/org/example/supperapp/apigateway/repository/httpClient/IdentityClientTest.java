package org.example.supperapp.apigateway.repository.httpClient;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.example.supperapp.apigateway.dto.request.IntrospectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class IdentityClientTest {

    @Test
    void invokesIntrospectionAsPostExchangeAndReadsBooleanResult() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"code\":200,\"message\":\"authenticated\",\"result\":true}")
                    .build());
        };
        WebClient webClient = WebClient.builder()
                .baseUrl("http://identity-service/identity")
                .exchangeFunction(exchangeFunction)
                .build();
        IdentityClient client = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient))
                .build()
                .createClient(IdentityClient.class);

        StepVerifier.create(client.introspect(IntrospectRequest.builder().token("valid-token").build()))
                .assertNext(response -> assertThat(response.getResult()).isTrue())
                .verifyComplete();

        assertThat(capturedRequest.get().method()).isEqualTo(HttpMethod.POST);
        assertThat(capturedRequest.get().url().getPath()).isEqualTo("/identity/auth/introspect");
    }
}
