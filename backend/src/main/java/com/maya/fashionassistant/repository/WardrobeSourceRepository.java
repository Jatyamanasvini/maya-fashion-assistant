package com.maya.fashionassistant.repository;

import com.maya.fashionassistant.entity.WardrobeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardrobeSourceRepository extends JpaRepository<WardrobeSource, Long> {
    List<WardrobeSource> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<WardrobeSource> findByIdAndUserId(Long id, Long userId);
}
