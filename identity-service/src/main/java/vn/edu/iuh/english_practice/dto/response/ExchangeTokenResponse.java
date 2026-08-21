package vn.edu.iuh.english_practice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)//theo gg
public class ExchangeTokenResponse {
    String accessToken;
    long expiresIn;
    String tokenType;
    String scope;
    String refreshToken;
}
