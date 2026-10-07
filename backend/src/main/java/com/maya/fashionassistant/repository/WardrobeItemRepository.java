package com.maya.fashionassistant.repository;

import com.maya.fashionassistant.entity.ItemCategory;
import com.maya.fashionassistant.entity.WardrobeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WardrobeItemRepository extends JpaRepository<WardrobeItem, Long> {
    List<WardrobeItem> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<WardrobeItem> findByIdAndUserId(Long id, Long userId);
    List<WardrobeItem> findByUserIdAndCategory(Long userId, ItemCategory category);
    List<WardrobeItem> findByUserIdAndIdIn(Long userId, Collection<Long> ids);
    long countByUserId(Long userId);
}
