package org.example.supperapp.examservice.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.example.supperapp.examservice.entity.comon.BaseDocument;
import org.example.supperapp.examservice.entity.enumeric.ContextType;

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
        name = "passages",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_exam_passage_group",
                        columnNames = {"exam_id", "group_number"}))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PassageEntity extends BaseDocument {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    ExamEntity exam;

    @Column(name = "group_number", nullable = false)
    Integer groupNumber;

    @Column(name = "part_number", nullable = false)
    Integer partNumber;

    @Column(name = "first_question_number", nullable = false)
    Integer firstQuestionNumber;

    @Column(name = "last_question_number", nullable = false)
    Integer lastQuestionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false, length = 24)
    ContextType contextType;

    @Column(columnDefinition = "TEXT")
    String content;

    @Column(name = "audio_url", length = 1000)
    String audioUrl;

    @Builder.Default
    @OneToMany(mappedBy = "passage", fetch = FetchType.LAZY)
    List<QuestionEntity> questions = new ArrayList<>();
}
