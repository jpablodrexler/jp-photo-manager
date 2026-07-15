package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public interface StoragePort {

    List<String> listFiles(String directoryPath);

    List<String> listSubDirectories(String directoryPath);

    boolean directoryExists(String path);

    boolean fileExists(String filePath);

    void createDirectory(String path);

    byte[] readFileBytes(String filePath) throws IOException;

    void copyFile(String sourcePath, String destinationPath) throws IOException;

    void moveFile(String sourcePath, String destinationPath) throws IOException;

    void deleteFile(String filePath) throws IOException;

    byte[] generateThumbnail(String filePath, int maxWidth, int maxHeight) throws IOException;

    String computeHash(String filePath) throws IOException;

    ImageRotation getImageRotation(String filePath) throws IOException;

    long getFileSize(String filePath);

    LocalDateTime getFileCreationDateTime(String filePath) throws IOException;

    LocalDateTime getFileModificationDateTime(String filePath) throws IOException;

    void convertPngToJpeg(String sourcePath, String destinationPath) throws IOException;

    ExifMetadata getExifMetadata(String filePath);

    boolean isVideoFile(String fileName);

    boolean isAudioFile(String fileName);

    boolean isPlaylistFile(String fileName);
}
