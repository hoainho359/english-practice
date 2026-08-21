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
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OutboundUserInfoResponse {
    String id;
    String email;
    String verifiedEmail;
    String name;
    String givenName;
    String familyName;
    String picture;
}
