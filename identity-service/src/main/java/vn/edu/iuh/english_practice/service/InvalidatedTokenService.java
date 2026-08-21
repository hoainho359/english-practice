package vn.edu.iuh.english_practice.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.iuh.english_practice.dto.request.LogoutRequest;
import vn.edu.iuh.english_practice.entity.InvalidatedToken;
import vn.edu.iuh.english_practice.exception.AppException;
import vn.edu.iuh.english_practice.exception.ErrorCode;
import vn.edu.iuh.english_practice.repository.InvalidatedTokenRepository;

import java.text.ParseException;
import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvalidatedTokenService {
    InvalidatedTokenRepository invalidatedTokenRepository;
    @NonFinal
    @Value("${jwt.signerKey}")
    String secretKey;

    public void logout(LogoutRequest logoutRequest) throws JOSEException, ParseException {

        JWSVerifier verifier = new MACVerifier(secretKey.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(logoutRequest.getToken());

        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        String jwtid = signedJWT.getJWTClaimsSet().getJWTID();
        boolean verify = signedJWT.verify(verifier);
        if (!(verify && expirationTime.after(new Date())))
            throw new AppException(ErrorCode.NOT_AUTHORIZE);

        if (invalidatedTokenRepository.existsById(jwtid))
            throw new AppException(ErrorCode.NOT_AUTHORIZE);

        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jwtid)
                .expiry(expirationTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);
    }
}
