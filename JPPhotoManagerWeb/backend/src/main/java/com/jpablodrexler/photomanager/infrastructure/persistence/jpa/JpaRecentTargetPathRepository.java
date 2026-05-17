package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

import com.jpablodrexler.photomanager.infrastructure.persistence.entity.RecentTargetPathEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaRecentTargetPathRepository extends JpaRepository<RecentTargetPathEntity, Long> {

    List<RecentTargetPathEntity> findAllByOrderByIdDesc();

    boolean existsByPath(String path);
}
