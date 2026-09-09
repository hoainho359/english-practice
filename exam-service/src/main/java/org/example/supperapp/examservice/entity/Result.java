package org.example.supperapp.examservice.entity;

import java.time.Instant;
import java.util.List;

import org.example.supperapp.examservice.entity.comon.BaseDocument;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Document(collection = "results")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Result extends BaseDocument {

    String userId;

    String examId;

    Instant startedAt;

    Instant submittedAt;

    Integer score;

    Integer totalQuestions;

    List<UserAnswer> answers;
}
