package vn.edu.iuh.english_practice.service;

import com.nimbusds.jose.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.english_practice.dto.request.UserRequest;
import vn.edu.iuh.english_practice.dto.response.UserResponse;
import vn.edu.iuh.english_practice.entity.Role;
import vn.edu.iuh.english_practice.entity.User;
import vn.edu.iuh.english_practice.repository.RoleRepository;
import vn.edu.iuh.english_practice.repository.UserRepository;
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
    public void deleteUser(String id){
        userRepository.deleteById(id);
    }
}
