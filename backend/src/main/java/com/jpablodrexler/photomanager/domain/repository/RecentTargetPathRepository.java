package com.jpablodrexler.photomanager.domain.repository;

import com.jpablodrexler.photomanager.domain.entity.RecentTargetPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecentTargetPathRepository extends JpaRepository<RecentTargetPath, Long> {

    List<RecentTargetPath> findAllByOrderByIdDesc();

    boolean existsByPath(String path);
}
