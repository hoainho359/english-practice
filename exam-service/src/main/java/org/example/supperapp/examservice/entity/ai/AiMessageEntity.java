package org.example.supperapp.examservice.entity.ai;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "ai_messages",
        indexes = @Index(name = "idx_ai_message_conversation_time", columnList = "conversation_id,created_at"),
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_ai_message_request_role",
                        columnNames = {"conversation_id", "request_id", "role"}))
public class AiMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    AiConversationEntity conversation;

    @Column(name = "request_id", nullable = false, length = 80)
    String requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    AiMessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 32)
    AiMessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    AiMessageStatus status;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    String content;

    @Column(name = "structured_payload", columnDefinition = "NVARCHAR(MAX)")
    String structuredPayload;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
}
