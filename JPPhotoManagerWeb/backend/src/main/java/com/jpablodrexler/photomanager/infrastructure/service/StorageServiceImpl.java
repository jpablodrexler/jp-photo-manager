package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {

    private static final int THUMBNAIL_MAX_WIDTH = 200;
    private static final int THUMBNAIL_MAX_HEIGHT = 150;

    @Override
    public List<String> listFiles(String directoryPath) {
        try (Stream<Path> stream = Files.list(Paths.get(directoryPath))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> isImageFile(p.getFileName().toString()))
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list files in {}", directoryPath, e);
            return List.of();
        }
    }

    @Override
    public List<String> listSubDirectories(String directoryPath) {
        try (Stream<Path> stream = Files.list(Paths.get(directoryPath))) {
            return stream
                    .filter(Files::isDirectory)
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list subdirectories in {}", directoryPath, e);
            return List.of();
        }
    }

    @Override
    public boolean directoryExists(String path) {
        return Files.isDirectory(Paths.get(path));
    }

    @Override
    public void createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            log.error("Failed to create directory {}", path, e);
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

    @Override
    public byte[] readFileBytes(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    @Override
    public void copyFile(String sourcePath, String destinationPath) throws IOException {
        Path dest = Paths.get(destinationPath);
        if (dest.getParent() != null) {
            Files.createDirectories(dest.getParent());
        }
        Files.copy(Paths.get(sourcePath), dest, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void moveFile(String sourcePath, String destinationPath) throws IOException {
        Path dest = Paths.get(destinationPath);
        if (dest.getParent() != null) {
            Files.createDirectories(dest.getParent());
        }
        Files.move(Paths.get(sourcePath), dest, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }

    @Override
    public BufferedImage loadImage(String filePath) throws IOException {
        BufferedImage image = ImageIO.read(Paths.get(filePath).toFile());
        if (image == null) {
            throw new IOException("Could not decode image: " + filePath);
        }
        return image;
    }

    @Override
    public byte[] generateThumbnail(String filePath, int maxWidth, int maxHeight) throws IOException {
        BufferedImage image = loadImage(filePath);
        ImageRotation rotation = getImageRotation(filePath);
        return generateThumbnail(image, maxWidth, maxHeight, rotation);
    }

    @Override
    public byte[] generateThumbnail(BufferedImage image, int maxWidth, int maxHeight, ImageRotation rotation) throws IOException {
        BufferedImage rotated = applyRotation(image, rotation);
        BufferedImage thumbnail = scaleThumbnail(rotated, maxWidth, maxHeight);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "JPEG", baos);
        return baos.toByteArray();
    }

    @Override
    public String computeHash(String filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Override
    public ImageRotation getImageRotation(String filePath) throws IOException {
        try {
            ImageMetadata metadata = Imaging.getMetadata(Paths.get(filePath).toFile());
            if (metadata instanceof JpegImageMetadata jpegMetadata) {
                TiffImageMetadata exif = jpegMetadata.getExif();
                if (exif != null) {
                    TiffField orientationField = exif.findField(TiffTagConstants.TIFF_TAG_ORIENTATION);
                    if (orientationField != null) {
                        int orientation = orientationField.getIntValueOrArraySum();
                        return switch (orientation) {
                            case 6 -> ImageRotation.ROTATE_90;
                            case 3 -> ImageRotation.ROTATE_180;
                            case 8 -> ImageRotation.ROTATE_270;
                            default -> ImageRotation.ROTATE_0;
                        };
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not read EXIF orientation for {}", filePath);
        }
        return ImageRotation.ROTATE_0;
    }

    @Override
    public long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public LocalDateTime getFileCreationDateTime(String filePath) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(Paths.get(filePath), BasicFileAttributes.class);
        return attrs.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Override
    public LocalDateTime getFileModificationDateTime(String filePath) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(Paths.get(filePath), BasicFileAttributes.class);
        return attrs.lastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private boolean isImageFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".gif")
                || lower.endsWith(".bmp") || lower.endsWith(".tiff")
                || lower.endsWith(".webp");
    }

    private BufferedImage applyRotation(BufferedImage image, ImageRotation rotation) {
        if (rotation == null || rotation == ImageRotation.ROTATE_0) return image;
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage rotated;
        AffineTransform transform = new AffineTransform();
        switch (rotation) {
            case ROTATE_90 -> {
                rotated = new BufferedImage(h, w, image.getType());
                transform.translate(h, 0);
                transform.rotate(Math.PI / 2);
            }
            case ROTATE_180 -> {
                rotated = new BufferedImage(w, h, image.getType());
                transform.translate(w, h);
                transform.rotate(Math.PI);
            }
            case ROTATE_270 -> {
                rotated = new BufferedImage(h, w, image.getType());
                transform.translate(0, w);
                transform.rotate(-Math.PI / 2);
            }
            default -> { return image; }
        }
        Graphics2D g = rotated.createGraphics();
        g.drawImage(image, transform, null);
        g.dispose();
        return rotated;
    }

    private BufferedImage scaleThumbnail(BufferedImage image, int maxWidth, int maxHeight) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        int targetWidth = (int) (originalWidth * ratio);
        int targetHeight = (int) (originalHeight * ratio);

        BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return thumbnail;
    }
}
