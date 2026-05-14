package com.jpablodrexler.photomanager.domain.port.out;

public interface ThumbnailPort {
    void saveThumbnail(String blobName, byte[] data);
    byte[] loadThumbnail(String blobName);
    void deleteThumbnail(String blobName);
    boolean thumbnailExists(String blobName);
}
