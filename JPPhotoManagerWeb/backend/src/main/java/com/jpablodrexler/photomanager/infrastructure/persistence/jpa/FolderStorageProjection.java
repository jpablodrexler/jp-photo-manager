package com.jpablodrexler.photomanager.infrastructure.persistence.jpa;

public interface FolderStorageProjection {
    String getFolderPath();
    Long getBytes();
}
