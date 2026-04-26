package com.jpablodrexler.photomanager.domain.service;

public interface ThumbnailStorageService {

    void saveThumbnail(String blobName, byte[] data);

    byte[] loadThumbnail(String blobName);

    void deleteThumbnail(String blobName);

    boolean thumbnailExists(String blobName);
}
