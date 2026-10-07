package com.maya.fashionassistant.repository;

import com.maya.fashionassistant.entity.Outfit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, Long> {
    List<Outfit> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Outfit> findByIdAndUserId(Long id, Long userId);
    Optional<Outfit> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
