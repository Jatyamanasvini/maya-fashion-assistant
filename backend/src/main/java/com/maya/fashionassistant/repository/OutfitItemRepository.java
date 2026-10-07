package com.maya.fashionassistant.repository;

import com.maya.fashionassistant.entity.OutfitItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitItemRepository extends JpaRepository<OutfitItem, Long> {
    List<OutfitItem> findByOutfitId(Long outfitId);
    List<OutfitItem> findByWardrobeItemId(Long wardrobeItemId);
}
