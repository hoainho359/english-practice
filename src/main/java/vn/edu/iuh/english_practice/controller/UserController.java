package vn.edu.iuh.english_practice.controller;

import com.nimbusds.jose.JOSEException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.english_practice.dto.request.AuthenticationRequest;
import vn.edu.iuh.english_practice.dto.request.IntropectTokenRequest;
import vn.edu.iuh.english_practice.dto.request.LogoutRequest;
import vn.edu.iuh.english_practice.dto.request.UserRequest;
import vn.edu.iuh.english_practice.dto.response.ApiResponse;
import vn.edu.iuh.english_practice.dto.response.UserResponse;
import vn.edu.iuh.english_practice.entity.User;
import vn.edu.iuh.english_practice.service.InvalidatedTokenService;
import vn.edu.iuh.english_practice.service.UserService;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {

    UserService userService;

    InvalidatedTokenService invalidatedTokenService;
    @PostMapping
    public User createUser(@RequestBody UserRequest userRequest) {
        return userService.createRequest(userRequest);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Debug userName " +authentication.getName());
        log.info("Debug getPrincipal " +authentication.getPrincipal());
        log.info("Debug getAuthorities " +authentication.getAuthorities());
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUsers());
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable String userId) {
        return userService.getUser(userId);
    }

//    @PutMapping("/{userId}")
//    public ResponseEntity<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest userUpdateRequest) {
//        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(userId, userUpdateRequest));
//    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> authenticated(@RequestBody AuthenticationRequest request){
        val authentication = userService.authentication(request);
        return  ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .success(true)
                        .code(200)
                        .message("authenticated")
                        .result(authentication)
                        .build()
                );
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> authenticated(@RequestBody LogoutRequest request) throws ParseException, JOSEException {
        invalidatedTokenService.logout(request);
        return  ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .success(true)
                        .code(200)
                        .message("logout success")
                        .result(null)
                        .build()
        );
    }
    @PostMapping("/intropect")
    public ResponseEntity<Boolean> authenticated(@RequestBody IntropectTokenRequest intropectTokenRequest) throws ParseException, JOSEException {
        System.out.println("log intropect: "+intropectTokenRequest.getToken());
        return  ResponseEntity.status(HttpStatus.OK).body(userService.intropectToken(intropectTokenRequest));
    }
}
