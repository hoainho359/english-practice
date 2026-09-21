package org.example.supperapp.examservice.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.example.supperapp.examservice.entity.comon.BaseDocument;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
        name = "questions",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_exam_question_number",
                        columnNames = {"exam_id", "question_number"}))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionEntity extends BaseDocument {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    ExamEntity exam;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passage_id")
    PassageEntity passage;

    @Column(name = "question_number", nullable = false)
    Integer questionNumber;

    @Column(columnDefinition = "TEXT")
    String content;

    @Builder.Default
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<OptionEntity> options = new ArrayList<>();

    @Column(name = "correct_option", length = 1)
    String correctOption;

    @Column(columnDefinition = "TEXT")
    String explanation;

    @Column(name = "image_url", length = 1000)
    String imageUrl;

    @Column(name = "image_base64", columnDefinition = "TEXT")
    String imageBase64;

    public void wireOptions() {
        options.forEach(option -> option.setQuestion(this));
    }
}
