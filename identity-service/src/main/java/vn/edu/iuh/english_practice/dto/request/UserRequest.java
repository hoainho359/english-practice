package vn.edu.iuh.english_practice.dto.request;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {
//    String id;
    String userName;
    String passWord;
    String firstName;
    LocalDate dob;
    String lastName;
    List<String> roles;
}
