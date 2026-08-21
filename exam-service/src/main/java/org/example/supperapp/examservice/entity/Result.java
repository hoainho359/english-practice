package org.example.supperapp.examservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Document(collection = "results")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Result extends org.example.supperapp.examservice.entity.comon.BaseDocument {

     String userId;

     String examId;

     Instant startedAt;

     Instant submittedAt;

     Integer score;

     Integer totalQuestions;

     List<UserAnswer> answers;
}