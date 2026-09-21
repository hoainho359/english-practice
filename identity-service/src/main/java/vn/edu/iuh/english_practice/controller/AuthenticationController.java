package vn.edu.iuh.english_practice.controller;

import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.english_practice.dto.request.AuthenticationRequest;
import vn.edu.iuh.english_practice.dto.request.GoogleIdTokenRequest;
import vn.edu.iuh.english_practice.dto.request.IntropectTokenRequest;
import vn.edu.iuh.english_practice.dto.request.LogoutRequest;
import vn.edu.iuh.english_practice.dto.response.ApiResponse;
import vn.edu.iuh.english_practice.dto.response.AuthenticationResponse;
import vn.edu.iuh.english_practice.service.AuthenticationService;
import vn.edu.iuh.english_practice.service.InvalidatedTokenService;

import java.text.ParseException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {
    AuthenticationService authenticationService;
    InvalidatedTokenService invalidatedTokenService;

    @PostMapping("/google/mobile")
    public ApiResponse<AuthenticationResponse> authenticateGoogleMobile(
            @Valid @RequestBody GoogleIdTokenRequest request) {
        AuthenticationResponse authenticationResponse =
                authenticationService.authenticateWithGoogleIdToken(request.getIdToken());

        return ApiResponse.<AuthenticationResponse>builder()
                .message("authenticated")
                .code(200)
                .success(true)
                .result(authenticationResponse)
                .build();
    }

    @PostMapping("/outbound/authentication")
    ApiResponse<AuthenticationResponse> outboundAuthenticate(
            @RequestParam("code") String code
    ){
        AuthenticationResponse authenticationResponse = authenticationService.outboundAuthenticate(code);

        return ApiResponse.<AuthenticationResponse>builder()
                .message(authenticationResponse.isSuccess() == true ? "authenticated":"unauthenticated")
                .code(200)
                .success(authenticationResponse.isSuccess())
                .result(authenticationResponse)
                .build();
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> authenticated(@RequestBody AuthenticationRequest request) {
        val authentication = authenticationService.authentication(request);
        log.info("Login controller reached");
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .success(true)
                        .code(200)
                        .message("authenticated")
                        .result(authentication)
                        .build()
        );
    }

    @PostMapping("/intropect")
    public ApiResponse<Boolean> authenticated(@RequestBody IntropectTokenRequest intropectTokenRequest) throws ParseException, JOSEException {
        System.out.println("log intropect: " + intropectTokenRequest.getToken());
        boolean b = authenticationService.intropectToken(intropectTokenRequest);
        return ApiResponse.<Boolean>builder()
                .code(200)
                .success(b)
                .message(b ? "authenticated" : "unauthenticated")
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> authenticated(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        invalidatedTokenService.logout(request);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .success(true)
                        .code(200)
                        .message("logout success")
                        .result(null)
                        .build()
        );
    }

}
