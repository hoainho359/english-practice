package vn.edu.iuh.english_practice.repository.httpClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.iuh.english_practice.dto.response.OutboundUserInfoResponse;

@FeignClient(url = "https://www.googleapis.com", name = "outbound-user")
public interface OutboundUserClient {
    @GetMapping("/oauth2/v1/userinfo")
    OutboundUserInfoResponse getUserInfo(@RequestParam("alt") String alt,
                                         @RequestParam("access_token") String accessToken);


}
