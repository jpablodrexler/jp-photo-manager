package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.enums.SocialMediaFormat;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.CropAssetRequest;
import com.jpablodrexler.photomanager.domain.port.in.asset.CropAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogFolderPort;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CropAssetUseCaseImpl implements CropAssetUseCase {

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;
    private final CatalogFolderPort catalogFolderPort;

    @Override
    @Transactional
    public Asset execute(long assetId, CropAssetRequest request) throws IOException {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NoSuchElementException("Asset not found: " + assetId));

        if (asset.getFolder() == null) {
            throw new NoSuchElementException("Asset has no folder: " + assetId);
        }

        SocialMediaFormat format = SocialMediaFormat.valueOf(request.formatKey());

        byte[] originalBytes = storagePort.readFileBytes(asset.getFullPath());
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (original == null) {
            throw new IOException("Cannot read image: " + asset.getFullPath());
        }

        validateCropBounds(request, original.getWidth(), original.getHeight());

        BufferedImage subimage = original.getSubimage(request.x(), request.y(), request.width(), request.height());

        int targetW = format.getTargetWidth();
        int targetH = format.getTargetHeight();
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(subimage, 0, 0, targetW, targetH, null);
        g2d.dispose();

        byte[] jpegBytes = toJpegBytes(scaled);

        String outputFileName = buildOutputFileName(asset.getFileName(), request.formatKey());
        String folderPath = asset.getFolder().getPath();
        String destPath = folderPath + "/" + outputFileName;

        Path tempFile = Files.createTempFile(UUID.randomUUID() + "_", "_" + outputFileName);
        try {
            Files.write(tempFile, jpegBytes);
            storagePort.copyFile(tempFile.toString(), destPath);
        } finally {
            Files.deleteIfExists(tempFile);
        }

        log.info("Cropped asset {} as {} → {}", assetId, request.formatKey(), outputFileName);
        return catalogFolderPort.createAsset(folderPath, outputFileName);
    }

    private void validateCropBounds(CropAssetRequest request, int imgWidth, int imgHeight) {
        if (request.x() < 0 || request.y() < 0
                || request.width() <= 0 || request.height() <= 0
                || (long) request.x() + request.width() > imgWidth
                || (long) request.y() + request.height() > imgHeight) {
            throw new IllegalArgumentException(
                    "Crop coordinates out of bounds: x=%d y=%d w=%d h=%d on image %dx%d"
                            .formatted(request.x(), request.y(), request.width(), request.height(), imgWidth, imgHeight));
        }
    }

    private String buildOutputFileName(String originalFileName, String formatKey) {
        int dot = originalFileName.lastIndexOf('.');
        String baseName = dot > 0 ? originalFileName.substring(0, dot) : originalFileName;
        return baseName + "_" + formatKey + ".jpg";
    }

    private byte[] toJpegBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.92f);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }
}
