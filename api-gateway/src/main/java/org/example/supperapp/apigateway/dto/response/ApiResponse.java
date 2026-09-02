package org.example.supperapp.apigateway.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = false)
public class ApiResponse <T>{
    int code;
    String message;
    T result;
}
