package org.example.supperapp.examservice.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
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

    @Column(name = "user_id", nullable = false, length = 160)
    String userId;

    // FIX: A TOEIC test spans multiple ExamEntity rows (one row per Part), so one exam_id cannot represent a result.
    // Store the stable business key used by the upload APIs instead.
    @Column(name = "exam_year", nullable = false)
    Integer examYear;

    @Column(name = "test_number", nullable = false)
    Integer testNumber;

    @Column(name = "started_at", nullable = false)
    Instant startedAt;

    @Column(name = "submitted_at", nullable = false)
    Instant submittedAt;

    @Column(nullable = false)
    Integer score;

    @Column(name = "total_questions", nullable = false)
    Integer totalQuestions;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_answers", joinColumns = @JoinColumn(name = "result_id"))
    @OrderColumn(name = "answer_order")
    List<UserAnswer> answers = new ArrayList<>();
}
