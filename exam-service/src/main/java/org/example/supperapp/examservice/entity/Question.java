package org.example.supperapp.examservice.entity;


import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Question {

     Integer questionNumber;

     String content;

     List<Option> options;

     String explanation;

     String imageBase64;
}