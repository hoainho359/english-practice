package org.example.supperapp.examservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAnswer {

    @Column(name = "question_number", nullable = false)
    Integer questionNumber;

    @Column(name = "selected_option", nullable = false, length = 1)
    String selectedOption;

    @Column(nullable = false)
    Boolean correct;
}
