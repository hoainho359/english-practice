package vn.edu.iuh.english_practice.repository.httpClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.edu.iuh.english_practice.dto.request.ExchangeTokenRequest;
import vn.edu.iuh.english_practice.dto.response.ExchangeTokenResponse;

@FeignClient(name = "outbound-identity",
        url = "https://oauth2.googleapis.com"
)
public interface OutboundIdentityClient {
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ExchangeTokenResponse exchangeToken(@RequestBody ExchangeTokenRequest exchangeTokenRequest);
}
