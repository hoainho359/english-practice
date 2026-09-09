package org.example.supperapp.examservice.entity;

import java.util.List;

import org.example.supperapp.examservice.entity.comon.BaseDocument;
import org.example.supperapp.examservice.entity.enumeric.PartType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Part extends BaseDocument {

    Integer partNumber;

    PartType type;

    String title;

    List<Question> questions;

    List<QuestionGroup> groups;
}
