package org.example.supperapp.examservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAnswer {

     Integer questionNumber;

     String selectedOption;

     Boolean correct;
}