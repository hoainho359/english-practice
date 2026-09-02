package vn.edu.iuh.english_practice.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
//@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)//theo gg
@ToString
public class ExchangeTokenRequest {
    String code;
    String client_id;
    String client_secret;
    String redirect_uri;
    String grant_type;
}
