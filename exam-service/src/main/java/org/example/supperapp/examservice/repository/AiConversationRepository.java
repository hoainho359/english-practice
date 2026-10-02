package org.example.supperapp.examservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.example.supperapp.examservice.entity.ai.AiConversationEntity;
import org.example.supperapp.examservice.entity.ai.AiConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity, UUID> {

    Optional<AiConversationEntity> findByIdAndUserIdAndStatus(UUID id, String userId, AiConversationStatus status);

    Page<AiConversationEntity> findByUserIdAndStatusOrderByLastMessageAtDesc(
            String userId, AiConversationStatus status, Pageable pageable);
}
