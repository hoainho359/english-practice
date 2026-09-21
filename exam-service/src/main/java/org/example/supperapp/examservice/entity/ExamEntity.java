package org.example.supperapp.examservice.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.example.supperapp.examservice.entity.comon.BaseDocument;
import org.example.supperapp.examservice.entity.enumeric.PartType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Entity
@Table(
        name = "exams",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_exam_year_test_part",
                        columnNames = {"exam_year", "test_number", "part_number"}))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamEntity extends BaseDocument {

    @Column(name = "exam_year", nullable = false)
    Integer year;

    @Column(name = "test_number", nullable = false)
    Integer testNumber;

    @Column(name = "part_number", nullable = false)
    Integer partNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    PartType type;

    @Column(nullable = false, length = 160)
    String title;

    @Builder.Default
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<QuestionEntity> questions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<PassageEntity> passages = new ArrayList<>();

    public void addQuestion(QuestionEntity question) {
        questions.add(question);
        question.setExam(this);
    }

    public void addPassage(PassageEntity passage) {
        passages.add(passage);
        passage.setExam(this);
        passage.getQuestions().forEach(question -> {
            if (!questions.contains(question)) {
                questions.add(question);
            }
            question.setExam(this);
            question.setPassage(passage);
        });
    }

    public void wireRelationships() {
        questions.forEach(question -> {
            question.setExam(this);
            question.wireOptions();
        });
        passages.forEach(passage -> {
            passage.setExam(this);
            passage.getQuestions().forEach(question -> {
                if (!questions.contains(question)) {
                    questions.add(question);
                }
                question.setExam(this);
                question.setPassage(passage);
                question.wireOptions();
            });
        });
    }
}
