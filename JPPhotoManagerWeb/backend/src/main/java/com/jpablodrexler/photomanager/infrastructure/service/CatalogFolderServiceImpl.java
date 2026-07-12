package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.model.CatalogChangeNotification;
import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetAudio;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.AudioMetadata;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;
import com.jpablodrexler.photomanager.domain.port.out.AssetAudioRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogFolderPort;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogFolderServiceImpl implements CatalogFolderPort {

    private static final int THUMBNAIL_MAX_WIDTH = 200;
    private static final int THUMBNAIL_MAX_HEIGHT = 150;

    private final AssetRepository assetRepository;
    private final AssetExifRepository assetExifRepository;
    private final AssetAudioRepository assetAudioRepository;
    private final FolderRepository folderRepository;
    private final StoragePort storageService;
    private final ThumbnailPort thumbnailStorageService;
    private final AudioMetadataService audioMetadataService;

    @Value("${photomanager.catalog-batch-size:1000}")
    int batchSize;

    @Transactional
    public void catalogFolder(String folderPath, Consumer<CatalogChangeNotification> callback,
                              Runnable heartbeatCallback, AtomicInteger processed, int total) {
        Folder folder = folderRepository.findByPath(folderPath).orElseGet(() -> {
            Folder f = new Folder();
            f.setPath(folderPath);
            Folder saved = folderRepository.save(f);
            if (callback != null) {
                callback.accept(new CatalogChangeNotification(ReasonEnum.FOLDER_CREATED, folderPath,
                        computePercent(processed.get(), total)));
            }
            return saved;
        });

        List<String> filesOnDisk = storageService.listFiles(folderPath);
        Set<String> fileNamesOnDisk = new HashSet<>();
        for (String filePath : filesOnDisk) {
            fileNamesOnDisk.add(Paths.get(filePath).getFileName().toString());
        }

        List<Asset> cataloguedAssets = assetRepository.findByFolder(folder);
        Set<String> cataloguedFileNames = new HashSet<>();
        for (Asset asset : cataloguedAssets) {
            cataloguedFileNames.add(asset.getFileName());
        }

        int assetsProcessed = 0;
        for (String filePath : filesOnDisk) {
            String fileName = Paths.get(filePath).getFileName().toString();
            if (!cataloguedFileNames.contains(fileName)) {
                try {
                    Asset asset = createAsset(folder, folderPath, fileName);
                    assetsProcessed++;
                    if (heartbeatCallback != null && assetsProcessed % batchSize == 0) {
                        heartbeatCallback.run();
                    }
                    if (callback != null) {
                        callback.accept(new CatalogChangeNotification(ReasonEnum.ASSET_CREATED, asset,
                                computePercent(processed.get(), total)));
                    }
                } catch (Exception e) {
                    log.error("Failed to catalog asset: {}", filePath, e);
                }
            }
        }

        for (Asset asset : cataloguedAssets) {
            if (!fileNamesOnDisk.contains(asset.getFileName())) {
                assetRepository.deleteById(asset.getAssetId());
                thumbnailStorageService.deleteThumbnail(asset.getThumbnailBlobName());
                if (callback != null) {
                    callback.accept(new CatalogChangeNotification(ReasonEnum.ASSET_DELETED, asset,
                            computePercent(processed.get(), total)));
                }
            }
        }

        processed.incrementAndGet();
    }

    @Transactional
    public Asset createAsset(String directoryPath, String fileName) {
        Folder folder = folderRepository.findByPath(directoryPath).orElseGet(() -> {
            Folder f = new Folder();
            f.setPath(directoryPath);
            return folderRepository.save(f);
        });
        return createAsset(folder, directoryPath, fileName);
    }

    // Runs within the caller's transaction (catalogFolder or createAsset).
    // The folder entity is already managed in that transaction, so no detached-entity issues.
    private Asset createAsset(Folder folder, String directoryPath, String fileName) {
        String filePath = directoryPath + "/" + fileName;
        try {
            if (storageService.isPlaylistFile(fileName)) {
                return createPlaylistAsset(folder, directoryPath, fileName);
            }
            if (storageService.isAudioFile(fileName)) {
                return createAudioAsset(folder, directoryPath, fileName);
            }
            Asset asset = new Asset();
            asset.setFolder(folder);
            asset.setFileName(fileName);
            asset.setFileSize(storageService.getFileSize(filePath));
            asset.setHash(storageService.computeHash(filePath));
            asset.setFileCreationDateTime(storageService.getFileCreationDateTime(filePath));
            asset.setFileModificationDateTime(storageService.getFileModificationDateTime(filePath));
            asset.setThumbnailCreationDateTime(LocalDateTime.now());
            asset.setImageRotation(storageService.getImageRotation(filePath));
            asset.setVideo(storageService.isVideoFile(fileName));
            asset.setFileType(asset.isVideo() ? FileType.VIDEO : FileType.IMAGE);

            byte[] thumbnail = storageService.generateThumbnail(filePath, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT);
            asset = assetRepository.save(asset);
            thumbnailStorageService.saveThumbnail(asset.getThumbnailBlobName(), thumbnail);

            saveExifMetadata(asset, filePath);

            return asset;
        } catch (IOException e) {
            log.error("Failed to create asset for {}", filePath, e);
            throw new RuntimeException("Failed to create asset", e);
        }
    }

    private Asset createPlaylistAsset(Folder folder, String directoryPath, String fileName) throws IOException {
        String filePath = directoryPath + "/" + fileName;

        Asset asset = new Asset();
        asset.setFolder(folder);
        asset.setFileName(fileName);
        asset.setFileSize(storageService.getFileSize(filePath));
        asset.setHash(storageService.computeHash(filePath));
        asset.setFileCreationDateTime(storageService.getFileCreationDateTime(filePath));
        asset.setFileModificationDateTime(storageService.getFileModificationDateTime(filePath));
        asset.setThumbnailCreationDateTime(LocalDateTime.now());
        asset.setFileType(FileType.PLAYLIST);

        asset = assetRepository.save(asset);
        thumbnailStorageService.saveThumbnail(asset.getThumbnailBlobName(), generatePlaylistPlaceholder());
        return asset;
    }

    private Asset createAudioAsset(Folder folder, String directoryPath, String fileName) throws IOException {
        String filePath = directoryPath + "/" + fileName;
        java.nio.file.Path path = Paths.get(filePath);

        Asset asset = new Asset();
        asset.setFolder(folder);
        asset.setFileName(fileName);
        asset.setFileSize(storageService.getFileSize(filePath));
        asset.setHash(storageService.computeHash(filePath));
        asset.setFileCreationDateTime(storageService.getFileCreationDateTime(filePath));
        asset.setFileModificationDateTime(storageService.getFileModificationDateTime(filePath));
        asset.setThumbnailCreationDateTime(LocalDateTime.now());
        asset.setFileType(FileType.AUDIO);

        asset = assetRepository.save(asset);

        Optional<byte[]> albumArt = audioMetadataService.extractAlbumArt(path);
        byte[] thumbnail;
        if (albumArt.isPresent()) {
            thumbnail = resizeToThumbnail(albumArt.get());
        } else {
            thumbnail = generateAudioPlaceholder();
        }
        thumbnailStorageService.saveThumbnail(asset.getThumbnailBlobName(), thumbnail);

        saveAudioMetadata(asset, path);

        return asset;
    }

    private void saveExifMetadata(Asset asset, String filePath) {
        try {
            ExifMetadata exif = storageService.getExifMetadata(filePath);
            AssetExif assetExif = assetExifRepository.findByAssetId(asset.getAssetId())
                    .orElseGet(() -> { AssetExif e = new AssetExif(); e.setAssetId(asset.getAssetId()); return e; });
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
            assetExifRepository.save(assetExif);
        } catch (Exception e) {
            log.warn("Failed to save EXIF metadata for asset {}", asset.getAssetId(), e);
        }
    }

    private void saveAudioMetadata(Asset asset, java.nio.file.Path filePath) {
        try {
            AudioMetadata metadata = audioMetadataService.extract(filePath);
            AssetAudio assetAudio = AssetAudio.builder()
                    .assetId(asset.getAssetId())
                    .title(metadata.title())
                    .artist(metadata.artist())
                    .album(metadata.album())
                    .durationSeconds(metadata.durationSeconds())
                    .bitrateKbps(metadata.bitrateKbps())
                    .sampleRateHz(metadata.sampleRateHz())
                    .build();
            assetAudioRepository.save(assetAudio);
        } catch (Exception e) {
            log.warn("Failed to save audio metadata for asset {}", asset.getAssetId(), e);
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

    private int computePercent(int processed, int total) {
        if (total == 0) return 100;
        return (int) ((double) processed / total * 100);
    }
}
