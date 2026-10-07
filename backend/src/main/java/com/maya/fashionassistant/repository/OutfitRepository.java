package com.maya.fashionassistant.repository;

import com.maya.fashionassistant.entity.Outfit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, Long> {
    List<Outfit> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Outfit> findByIdAndUserId(Long id, Long userId);

    // Items are fetched eagerly so callers can read them outside the persistence context
    // (e.g. to apply "another option" / refinement constraints from the previous outfit).
    @EntityGraph(attributePaths = "items")
    Optional<Outfit> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
