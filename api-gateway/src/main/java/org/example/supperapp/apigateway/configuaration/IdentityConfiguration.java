package org.example.supperapp.apigateway.configuaration;

import org.example.supperapp.apigateway.repository.httpClient.IdentityClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

//have class because prj using Reactive GateWay not independent on Open-Fient
@Configuration
public class IdentityConfiguration {
    @Bean
    WebClient webClient(){
        return WebClient.builder()
                .baseUrl("http://localhost:8080/identity")
                .build();
    }
    @Bean
    IdentityClient identityClient(WebClient webClient){
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();

        return factory.createClient(IdentityClient.class);

    }
}
