package vn.edu.iuh.english_practice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleAuthorizationCodeRequest {
    @NotBlank
    String code;

    @NotBlank
    String codeVerifier;

    @NotBlank
    String redirectUri;

    @NotBlank
    @Pattern(regexp = "WEB|ANDROID|IOS")
    String platform;
}
