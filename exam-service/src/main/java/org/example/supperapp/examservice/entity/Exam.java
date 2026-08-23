package org.example.supperapp.examservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.supperapp.examservice.entity.comon.BaseDocument;
import org.example.supperapp.examservice.entity.enumeric.PartType;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document(collection = "exams")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Exam extends BaseDocument {
     Integer year;

     Integer testNumber;

     Integer partNumber;

     PartType type;

     String title;

     List<Question> questions;

     List<QuestionGroup> groups;
}