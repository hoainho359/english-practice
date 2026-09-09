package org.example.supperapp.examservice.entity;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionGroup {
    // ussing for part  6, 7
    Integer groupNumber;

    String passage;

    String audioUrl;

    List<Question> questions;
}
