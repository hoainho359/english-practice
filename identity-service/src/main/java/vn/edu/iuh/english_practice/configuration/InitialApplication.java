package vn.edu.iuh.english_practice.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.iuh.english_practice.dto.request.UserRequest;
import vn.edu.iuh.english_practice.repository.UserRepository;
import vn.edu.iuh.english_practice.service.UserService;

import java.time.LocalDate;
import java.util.List;
@Configuration
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class InitialApplication {
    PasswordEncoder passwordEncoder;
    UserRepository userRepository;
    @Bean
    @ConditionalOnProperty(
            prefix = "spring",
            value = "datasource.driver-class-name",
            havingValue = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    )
    ApplicationRunner applicationRunner(UserService userService){
        return args -> {
            //admin
            if(userRepository.findByUserName("admin").isEmpty()){
                UserRequest userRequestAdmin = UserRequest.builder()
                        .userName("admin")
                        .passWord(passwordEncoder.encode("admin"))
                        .dob(LocalDate.now())
                        .firstName("admin")
                        .lastName("admin")
                        .roles(List.of("admin"))
                        .build();
                 userService.createRequest(userRequestAdmin);
            }
            if(userRepository.findByUserName("user").isEmpty()){
                UserRequest userRequestUser = UserRequest.builder()
                        .userName("user")
                        .passWord(passwordEncoder.encode("user"))
                        .dob(LocalDate.now())
                        .firstName("user")
                        .lastName("user")
                        .roles(List.of("user"))
                        .build();
                userService.createRequest(userRequestUser);
            }
            log.info("Application initialization completed .....");
        };

    }
}
