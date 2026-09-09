package org.example.supperapp.examservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Option {

    String label;

    String text;

    Boolean correct;
}
