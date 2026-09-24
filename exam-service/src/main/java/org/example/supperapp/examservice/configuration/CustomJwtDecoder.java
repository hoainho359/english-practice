package org.example.supperapp.examservice.configuration;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    private final NimbusJwtDecoder delegate;

    public CustomJwtDecoder(@Value("${jwt.signer-key}") String signerKey) {
        SecretKeySpec secretKey =
                new SecretKeySpec(signerKey.getBytes(StandardCharsets.UTF_8), MacAlgorithm.HS512.getName());
        delegate = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
        delegate.setJwtValidator(JwtValidators.createDefaultWithIssuer("english_practice"));
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        // FIX: Parsing a JWT is not authentication. Verify signature, expiry and issuer before using its subject.
        return delegate.decode(token);
    }
}
