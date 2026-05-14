package com.jpablodrexler.photomanager.domain.port.out;

import java.util.List;

public interface RecentTargetPathRepositoryPort {
    List<String> findAllOrderByIdDesc();
    boolean existsByPath(String path);
    void save(String path);
    void deleteOldestBeyond(int maxCount);
}
