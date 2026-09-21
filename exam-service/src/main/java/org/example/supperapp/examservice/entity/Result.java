package org.example.supperapp.examservice.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.example.supperapp.examservice.entity.comon.BaseDocument;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Entity
@Table(name = "exam_results")
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

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_answers", joinColumns = @JoinColumn(name = "result_id"))
    List<UserAnswer> answers = new ArrayList<>();
}
