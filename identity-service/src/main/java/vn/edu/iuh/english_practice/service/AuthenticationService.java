package vn.edu.iuh.english_practice.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import vn.edu.iuh.english_practice.dto.request.AuthenticationRequest;
import vn.edu.iuh.english_practice.dto.request.ExchangeTokenRequest;
import vn.edu.iuh.english_practice.dto.request.IntropectTokenRequest;
import vn.edu.iuh.english_practice.dto.response.ApiResponse;
import vn.edu.iuh.english_practice.dto.response.AuthenticationResponse;
import vn.edu.iuh.english_practice.dto.response.ExchangeTokenResponse;
import vn.edu.iuh.english_practice.dto.response.OutboundUserInfoResponse;
import vn.edu.iuh.english_practice.configuration.GoogleIdTokenVerifier;
import vn.edu.iuh.english_practice.entity.Permission;
import vn.edu.iuh.english_practice.entity.Role;
import vn.edu.iuh.english_practice.entity.User;
import vn.edu.iuh.english_practice.exception.AppException;
import vn.edu.iuh.english_practice.exception.ErrorCode;
import vn.edu.iuh.english_practice.repository.RoleRepository;
import vn.edu.iuh.english_practice.repository.UserRepository;
import vn.edu.iuh.english_practice.repository.httpClient.OutboundIdentityClient;
import vn.edu.iuh.english_practice.repository.httpClient.OutboundUserClient;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    @NonFinal
    @Value("${jwt.signerKey}")
    String secretKey;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    OutboundIdentityClient outboundIdentityClient;
    OutboundUserClient outboundUserClient;
    RoleRepository roleRepository;
    GoogleIdTokenVerifier googleIdTokenVerifier;

//    @Value("${outbound.identity.redirectUri}")
//    @NonFinal
//    String redirectUri;

    @Value("${outbound.identity.clientId}")
    @NonFinal
    String clientId;
    @Value("${outbound.identity.clientSecret}")
    @NonFinal
    String clientSecret;


    public AuthenticationResponse outboundAuthenticate(String code){
        //đây exchange sang gg dùng feign client
      try {
          ExchangeTokenRequest exchangeTokenRequest = ExchangeTokenRequest.builder()
                  .redirect_uri("http://localhost:5173/authenticate")
                  .client_id(clientId)
                  .grant_type("authorization_code")
                  .code(code)
                  .client_secret(clientSecret)
                  .build();
          log.info("outbound rq: {}", exchangeTokenRequest.toString());
          ExchangeTokenResponse exchangeTokenResponse = outboundIdentityClient.exchangeToken(exchangeTokenRequest);
          log.info("outbound rq: {}", exchangeTokenResponse.toString());
          //sau khi user continue gg onboad vao db
          OutboundUserInfoResponse json = outboundUserClient.getUserInfo("json", exchangeTokenResponse.getAccessToken());

          log.info("user info {}", json);
          return authenticateGoogleUser(json);
      }catch (FeignException e){
          log.error("Google HTTP status: {}", e.status());
          log.error("Google response body: {}", e.contentUTF8());
          throw e;
      }
    }

    public AuthenticationResponse authenticateWithGoogleIdToken(String idToken) {
        try {
            Jwt googleToken = googleIdTokenVerifier.verify(idToken);
            Boolean emailVerified = googleToken.getClaimAsBoolean("email_verified");
            String email = googleToken.getClaimAsString("email");
            String providerId = googleToken.getSubject();

            if (!Boolean.TRUE.equals(emailVerified)
                    || providerId == null
                    || providerId.isBlank()
                    || email == null
                    || email.isBlank()) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            OutboundUserInfoResponse googleUser = OutboundUserInfoResponse.builder()
                    .id(providerId)
                    .email(email)
                    .name(googleToken.getClaimAsString("name"))
                    .givenName(googleToken.getClaimAsString("given_name"))
                    .familyName(googleToken.getClaimAsString("family_name"))
                    .picture(googleToken.getClaimAsString("picture"))
                    .build();

            return authenticateGoogleUser(googleUser);
        } catch (JwtException exception) {
            log.warn("Rejected invalid Google ID token: {}", exception.getMessage());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private AuthenticationResponse authenticateGoogleUser(
            OutboundUserInfoResponse googleUser) {
        User user = userRepository.findByProviderId(googleUser.getId())
                .orElseGet(() -> createGoogleUser(googleUser));

        return AuthenticationResponse.builder()
                .isSuccess(true)
                .token(generateToken(user))
                .build();
    }

    private User createGoogleUser(OutboundUserInfoResponse googleUser) {
        Role userRole = roleRepository.findByName("user")
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        User user = User.builder()
                .firstName(googleUser.getGivenName())
                .lastName(googleUser.getFamilyName())
                .userName(googleUser.getEmail())
                .roles(Set.of(userRole))
                .provierId(googleUser.getId())
                .build();

        return userRepository.save(user);
    }
    public String generateToken(User user) {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .subject(user.getUserName())
                .issueTime(new Date())
                .expirationTime(new Date(
                                Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                        )
                )
                .issuer("english_practice")
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        try {
            jwsObject.sign(new MACSigner(secretKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            for (Role role : user.getRoles()) {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    for (Permission permission : role.getPermissions()) {
                        stringJoiner.add(permission.getName());
                    }
                }

            }
        }
        return stringJoiner.toString();
    }

    public AuthenticationResponse authentication(AuthenticationRequest authenticationRequest) {
        Optional<User> first = userRepository.findByUserName(authenticationRequest.getUserName())
                .stream().findFirst();
        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .token("")
                .isSuccess(false)
                .build();
        if (first.isEmpty()) return authenticationResponse;

        User user = first.get();
        if (!passwordEncoder.matches(authenticationRequest.getPassWord(), user.getPassWord()))
            return authenticationResponse;
        else
           return AuthenticationResponse.builder()
                   .isSuccess(true)
                   .token(generateToken(user))
                   .build();

    }

    public SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(secretKey);
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }
    public boolean intropectToken(IntropectTokenRequest tokenRequest ) throws ParseException, JOSEException {
        String token = tokenRequest.getToken();
        boolean isValid = true;
        try {
            SignedJWT signedJWT = verifyToken(token);
        }catch (AppException e){
            isValid = false;
        }
        return isValid;
    }
}
