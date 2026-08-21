package org.example.supperapp.examservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableMongoAuditing
/*
Tự động điền thời gian:
 Tự động gán giá trị thời gian lúc tạo (@CreatedDate) và
 lúc cập nhật gần nhất (@LastModifiedDate).
 Tự động ghi lại tên người tạo (@CreatedBy) hoặc
  người chỉnh sửa cuối cùng (@LastModifiedBy) thông qua bean
 */
public class SecurityConfig {
    private final CustomJwtDecoder customJwtDecoder;

    private String[] PUBLIC_ENTRY_POINT = {"/**"};
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity){
        try {
            return  httpSecurity.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> authorizationManagerRequestMatcherRegistry
                            .requestMatchers(HttpMethod.POST, PUBLIC_ENTRY_POINT).permitAll()
                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer ->
                            httpSecurityOAuth2ResourceServerConfigurer.jwt(jwtConfigurer -> jwtConfigurer
                                            .decoder(customJwtDecoder)
                                            .jwtAuthenticationConverter(jwtAuthenticationConverter())
                                    )
                                    .authenticationEntryPoint(new JwtAuthenticationEntryPoint())

                    )
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
