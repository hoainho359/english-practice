package org.example.supperapp.examservice.entity.ai;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
        name = "ai_conversations",
        indexes = @Index(name = "idx_ai_conversation_user_last", columnList = "user_id,last_message_at"))
public class AiConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id", nullable = false, length = 160)
    String userId;

    @Column(nullable = false, columnDefinition = "NVARCHAR(160)")
    String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    AiConversationStatus status;

    @Column(name = "last_message_preview", columnDefinition = "NVARCHAR(300)")
    String lastMessagePreview;

    @Column(name = "message_count", nullable = false)
    long messageCount;

    @Column(name = "last_message_at", nullable = false)
    Instant lastMessageAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    Long version;
}
