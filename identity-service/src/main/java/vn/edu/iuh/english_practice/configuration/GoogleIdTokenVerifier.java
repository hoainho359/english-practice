package vn.edu.iuh.english_practice.configuration;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdTokenVerifier {
    private static final String GOOGLE_JWK_SET_URI =
            "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder jwtDecoder;

    public GoogleIdTokenVerifier(
            @Value("${outbound.identity.clientId}") String googleWebClientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(GOOGLE_JWK_SET_URI)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator = token -> {
            String issuer = token.getIssuer() == null
                    ? null
                    : token.getIssuer().toString();

            if (GOOGLE_ISSUERS.contains(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "Invalid Google token issuer", null));
        };

        OAuth2TokenValidator<Jwt> audienceValidator = token -> {
            List<String> audiences = token.getAudience();
            if (googleWebClientId != null
                    && !googleWebClientId.isBlank()
                    && audiences.contains(googleWebClientId)) {
                return OAuth2TokenValidatorResult.success();
            }

            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "invalid_token", "Invalid Google token audience", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                issuerValidator,
                audienceValidator));
        this.jwtDecoder = decoder;
    }

    public Jwt verify(String idToken) {
        return jwtDecoder.decode(idToken);
    }
}
