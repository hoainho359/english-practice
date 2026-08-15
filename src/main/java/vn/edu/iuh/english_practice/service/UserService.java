package vn.edu.iuh.english_practice.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.english_practice.dto.request.AuthenticationRequest;
import vn.edu.iuh.english_practice.dto.request.IntropectTokenRequest;
import vn.edu.iuh.english_practice.dto.request.UserRequest;
import vn.edu.iuh.english_practice.dto.response.AuthenticationResponse;
import vn.edu.iuh.english_practice.dto.response.UserResponse;
import vn.edu.iuh.english_practice.entity.Role;
import vn.edu.iuh.english_practice.entity.User;
import vn.edu.iuh.english_practice.exception.AppException;
import vn.edu.iuh.english_practice.exception.ErrorCode;
import vn.edu.iuh.english_practice.repository.InvalidatedTokenRepository;
import vn.edu.iuh.english_practice.repository.RoleRepository;
import vn.edu.iuh.english_practice.repository.UserRepository;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
     UserRepository userRepository;
     RoleRepository roleRepository;
     PasswordEncoder passwordEncoder;
     InvalidatedTokenRepository invalidatedTokenRepository;
     @NonFinal
    @Value("${jwt.signerKey}")
    String signerKey;
    public User createRequest(UserRequest userRequest){
        Set<Role> roles = new HashSet<>(
                roleRepository.findAllByNameIn(userRequest.getRoles())
        );
        System.out.println(roles);
        User user = User.builder()
                .userName(userRequest.getUserName())
                .passWord(userRequest.getPassWord())
                .firstName(userRequest.getFirstName())
                .lastName(userRequest.getLastName())
                .dob(userRequest.getDob())
                .roles(roles)
                .build();
        return userRepository.save(user);
    }
//    @PreAuthorize("hasRole('USER')")
    public List<UserResponse> getUsers(){
        return userRepository.findAll().stream()
                .map(user -> UserResponse.builder()
                        .userName(user.getUserName())
                        .passWord(user.getPassWord())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .Dob(user.getDob())
                        .roles(user.getRoles().stream()
                                .map(role -> role.getName())
                                .collect(Collectors.toSet())
                        )
                        .build()
                ).toList();
    }
    @PostAuthorize("returnObject.userName == authentication.getName()")
    public UserResponse getUser(String userId) {
        // 1. Lấy ra một lần duy nhất
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Map từ đối tượng đã lấy được
        List<String> authorities = new ArrayList<>();

        user.getRoles().forEach(role -> {
            authorities.add("ROLE_" + role.getName().toUpperCase());

            role.getPermissions().forEach(permission ->
                    authorities.add(permission.getName()));
        });
        return UserResponse.builder()
                .userName(user.getUserName())
                .passWord(user.getPassWord())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .Dob(user.getDob())
                .roles(new HashSet<>(authorities))
                .build();
    }
//    public UserResponse updateUser(String userId, UserUpdateRequest userUpdateRequest){
////        User user = getUser(userId);
//        User user = userRepository.findById(userId).get();
//        user.setPassWord(passwordEncoder.encode(userUpdateRequest.getPassWord()));
//        user.setFirstName(userUpdateRequest.getFirstName());
//        user.setLastName(userUpdateRequest.getLastName());
//        user.setDob(userUpdateRequest.getDob());
//        List<Role> allByNameIn = roleRepository.findAllByNameIn(userUpdateRequest.getRoles());
//        user.setRoles(new HashSet<>(allByNameIn));
//         userRepository.save(user);
//         var roles = user.getRoles().stream()
//                 .map(role -> "ROLE_"+role.getName().toUpperCase())
//                 .toList();
//         return UserResponse.builder()
//                 .firstName(user.getFirstName())
//                 .lastName(user.getLastName())
//                 .userName(user.getUserName())
//                 .passWord(user.getPassWord())
//                 .roles(new HashSet<>(roles))
//                 .Dob(user.getDob())
//                 .build();
//    }
    public void deleteUser(String id){
        userRepository.deleteById(id);
    }

    public AuthenticationResponse authentication(AuthenticationRequest request){
        Optional<User> first = userRepository.findAllByUserName(request.getUserName())
                .stream().findFirst();

        User user =  first.isEmpty()
                ? null
                :first.get();
        if (user == null) throw new AppException(ErrorCode.WRONG_ACCOUNT);
        PasswordEncoder encoder = new BCryptPasswordEncoder(10);
        if( encoder.matches(request.getPassWord(), user.getPassWord()) == true) {
            try {
                return AuthenticationResponse.builder()
                .isSuccess(true)
                .token(generateToken(user))
                .build();
            } catch (KeyLengthException e) {
                throw new RuntimeException(e);
            }
        }
        throw new AppException(ErrorCode.WRONG_ACCOUNT);
    }

    public String generateToken(User user) throws KeyLengthException {
        //using SignedJWT by convinent SignedJwt = header + claimSet + secrect
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUserName())
//                .claim("scope", user.getScope())
                .issuer("springboot_3")
                .issueTime(new Date())
                .jwtID(UUID.randomUUID().toString())//ussing for save token expiried
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
                .build();
        //Payload payload = new Payload(claimsSet.toJSONObject());
//        JWSObject object = new JWSObject(header, payload);
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        try {
            signedJWT.sign(new MACSigner(signerKey.getBytes()));
            return  signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean intropectToken(IntropectTokenRequest intropectTokenRequest) throws JOSEException, ParseException {
      JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
      SignedJWT signedJWT = SignedJWT.parse(intropectTokenRequest.getToken());
        /**
         * phải chk them co o db chua vi logout r khong cho dung nua
         *
         */
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        String jwtid = signedJWT.getJWTClaimsSet().getJWTID();
        boolean verify = signedJWT.verify(verifier);
        if (!(verify && expirationTime.after(new Date())))
            throw new AppException(ErrorCode.NOT_AUTHORIZE);

        if (invalidatedTokenRepository.existsById(jwtid))
            throw new AppException(ErrorCode.NOT_AUTHORIZE);

        return signedJWT.verify(verifier) && expirationTime.after(new Date());
    }
}
