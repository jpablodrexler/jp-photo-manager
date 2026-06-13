package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetAudio;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.AudioMetadata;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.infrastructure.service.AudioMetadataService;
import com.jpablodrexler.photomanager.infrastructure.service.StorageServiceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class CatalogAssetItemProcessor implements ItemProcessor<Path, CatalogBatchItem> {

    private static final int THUMBNAIL_MAX_WIDTH = 200;
    private static final int THUMBNAIL_MAX_HEIGHT = 150;

    private final StoragePort storagePort;
    private final AudioMetadataService audioMetadataService;

    @Override
    public CatalogBatchItem process(Path filePath) throws Exception {
        String filePathStr = filePath.toAbsolutePath().toString();
        String fileName = filePath.getFileName().toString();
        try {
            if (StorageServiceAdapter.isPlaylistFile(fileName)) {
                return processPlaylist(filePathStr, fileName);
            }
            if (StorageServiceAdapter.isAudioFile(fileName)) {
                return processAudio(filePathStr, fileName, filePath);
            }
            return processImage(filePathStr, fileName);
        } catch (Exception e) {
            log.error("Failed to process asset: {}", filePathStr, e);
            throw e;
        }
    }

    private CatalogBatchItem processImage(String filePathStr, String fileName) throws IOException {
        Asset asset = new Asset();
        asset.setFileName(fileName);
        asset.setFileSize(storagePort.getFileSize(filePathStr));
        asset.setHash(storagePort.computeHash(filePathStr));
        asset.setFileCreationDateTime(storagePort.getFileCreationDateTime(filePathStr));
        asset.setFileModificationDateTime(storagePort.getFileModificationDateTime(filePathStr));
        asset.setThumbnailCreationDateTime(LocalDateTime.now());
        asset.setImageRotation(storagePort.getImageRotation(filePathStr));
        asset.setFileType(FileType.IMAGE);

        byte[] thumbnailData = storagePort.generateThumbnail(filePathStr, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
        AssetExif assetExif = buildExif(filePathStr);

        return new CatalogBatchItem(asset, thumbnailData, assetExif, null);
    }

    private CatalogBatchItem processPlaylist(String filePathStr, String fileName) throws IOException {
        Asset asset = new Asset();
        asset.setFileName(fileName);
        asset.setFileSize(storagePort.getFileSize(filePathStr));
        asset.setHash(storagePort.computeHash(filePathStr));
        asset.setFileCreationDateTime(storagePort.getFileCreationDateTime(filePathStr));
        asset.setFileModificationDateTime(storagePort.getFileModificationDateTime(filePathStr));
        asset.setThumbnailCreationDateTime(LocalDateTime.now());
        asset.setFileType(FileType.PLAYLIST);

        return new CatalogBatchItem(asset, generatePlaylistPlaceholder(), null, null);
    }

    private CatalogBatchItem processAudio(String filePathStr, String fileName, Path filePath) throws IOException {
        Asset asset = new Asset();
        asset.setFileName(fileName);
        asset.setFileSize(storagePort.getFileSize(filePathStr));
        asset.setHash(storagePort.computeHash(filePathStr));
        asset.setFileCreationDateTime(storagePort.getFileCreationDateTime(filePathStr));
        asset.setFileModificationDateTime(storagePort.getFileModificationDateTime(filePathStr));
        asset.setThumbnailCreationDateTime(LocalDateTime.now());
        asset.setFileType(FileType.AUDIO);

        Optional<byte[]> albumArt = audioMetadataService.extractAlbumArt(filePath);
        byte[] thumbnailData = albumArt.isPresent()
                ? resizeToThumbnail(albumArt.get())
                : generateAudioPlaceholder();

        AudioMetadata metadata = audioMetadataService.extract(filePath);
        AssetAudio assetAudio = AssetAudio.builder()
                .title(metadata.title())
                .artist(metadata.artist())
                .album(metadata.album())
                .durationSeconds(metadata.durationSeconds())
                .bitrateKbps(metadata.bitrateKbps())
                .sampleRateHz(metadata.sampleRateHz())
                .build();

        return new CatalogBatchItem(asset, thumbnailData, null, assetAudio);
    }

    private AssetExif buildExif(String filePathStr) {
        try {
            ExifMetadata exif = storagePort.getExifMetadata(filePathStr);
            AssetExif assetExif = new AssetExif();
            assetExif.setCameraMake(exif.cameraMake());
            assetExif.setCameraModel(exif.cameraModel());
            assetExif.setLensModel(exif.lensModel());
            assetExif.setExposureTime(exif.exposureTime());
            assetExif.setFNumber(exif.fNumber());
            assetExif.setIsoSpeed(exif.isoSpeed());
            assetExif.setFocalLength(exif.focalLength());
            assetExif.setDateTaken(exif.dateTaken());
            assetExif.setWidthPixels(exif.widthPixels());
            assetExif.setHeightPixels(exif.heightPixels());
            assetExif.setGpsLatitude(exif.gpsLatitude());
            assetExif.setGpsLongitude(exif.gpsLongitude());
            assetExif.setRawExif(exif.rawExif());
            return assetExif;
        } catch (Exception e) {
            log.warn("Failed to read EXIF metadata from {}", filePathStr, e);
            return null;
        }
    }

    private byte[] resizeToThumbnail(byte[] imageData) throws IOException {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageData));
        if (src == null) return generateAudioPlaceholder();
        BufferedImage resized = new BufferedImage(THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(src, 0, 0, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", out);
        return out.toByteArray();
    }

    private byte[] generateAudioPlaceholder() throws IOException {
        BufferedImage image = new BufferedImage(THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(48, 48, 48));
        g.fillRect(0, 0, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
        g.setColor(new Color(180, 180, 180));
        g.setFont(g.getFont().deriveFont(48f));
        g.drawString("♫", THUMBNAIL_MAX_WIDTH / 2 - 20, THUMBNAIL_MAX_HEIGHT / 2 + 18);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private byte[] generatePlaylistPlaceholder() throws IOException {
        BufferedImage image = new BufferedImage(THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(30, 30, 60));
        g.fillRect(0, 0, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
        g.setColor(new Color(180, 180, 220));
        g.setFont(g.getFont().deriveFont(40f));
        g.drawString("≡♫", THUMBNAIL_MAX_WIDTH / 2 - 28, THUMBNAIL_MAX_HEIGHT / 2 + 16);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}
