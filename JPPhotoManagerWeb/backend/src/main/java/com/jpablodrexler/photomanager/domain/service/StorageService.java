package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface StorageService {

    List<String> listFiles(String directoryPath);

    List<String> listSubDirectories(String directoryPath);

    boolean directoryExists(String path);

    void createDirectory(String path);

    byte[] readFileBytes(String filePath) throws IOException;

    void copyFile(String sourcePath, String destinationPath) throws IOException;

    void moveFile(String sourcePath, String destinationPath) throws IOException;

    void deleteFile(String filePath) throws IOException;

    BufferedImage loadImage(String filePath) throws IOException;

    byte[] generateThumbnail(String filePath, int maxWidth, int maxHeight) throws IOException;

    byte[] generateThumbnail(BufferedImage image, int maxWidth, int maxHeight, ImageRotation rotation) throws IOException;

    String computeHash(String filePath) throws IOException;

    ImageRotation getImageRotation(String filePath) throws IOException;

    long getFileSize(String filePath);

    java.time.LocalDateTime getFileCreationDateTime(String filePath) throws IOException;

    java.time.LocalDateTime getFileModificationDateTime(String filePath) throws IOException;

    ExifMetadata getExifMetadata(String filePath);
}
