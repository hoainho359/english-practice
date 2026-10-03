package org.example.supperapp.examservice.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.example.supperapp.examservice.entity.ai.AiMessageEntity;
import org.example.supperapp.examservice.entity.ai.AiMessageRole;
import org.example.supperapp.examservice.entity.ai.AiMessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMessageRepository extends JpaRepository<AiMessageEntity, UUID> {

    Optional<AiMessageEntity> findFirstByRequestIdAndConversationUserIdOrderByCreatedAtAsc(
            String requestId, String userId);

    Optional<AiMessageEntity> findFirstByConversationIdAndRoleOrderByCreatedAtAsc(
            UUID conversationId, AiMessageRole role);

    Optional<AiMessageEntity> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    Optional<AiMessageEntity> findByConversationIdAndRequestIdAndRole(
            UUID conversationId, String requestId, AiMessageRole role);

    Slice<AiMessageEntity> findByConversationIdAndStatusOrderByCreatedAtDesc(
            UUID conversationId, AiMessageStatus status, Pageable pageable);

    Slice<AiMessageEntity> findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
            UUID conversationId, Instant before, Pageable pageable);

    Slice<AiMessageEntity> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
}
