package org.example.supperapp.examservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.supperapp.examservice.entity.comon.BaseDocument;
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

     String title;

     Integer duration;

     List<Part> parts;
}