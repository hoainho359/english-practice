package vn.edu.iuh.english_practice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@Builder
public class UserResponse {
    String userName;
    String passWord;
    String firstName;
    String lastName;
    LocalDate Dob;
    Set<String> roles;
}
