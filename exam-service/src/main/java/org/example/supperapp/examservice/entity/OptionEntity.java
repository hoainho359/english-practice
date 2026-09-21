package org.example.supperapp.examservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "question_options",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_question_option_label",
                        columnNames = {"question_id", "option_label"}))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OptionEntity extends BaseDocument {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    QuestionEntity question;

    @Column(name = "option_label", nullable = false, length = 1)
    String label;

    @Column(columnDefinition = "TEXT")
    String text;
}
