package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.CropRegion;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogFolderPort;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CropAssetUseCaseImplTest {

    @Mock AssetRepository assetRepository;
    @Mock StoragePort storagePort;
    @Mock CatalogFolderPort catalogFolderPort;
    @InjectMocks CropAssetUseCaseImpl sut;

    private Folder folder;
    private Asset asset;

    @BeforeEach
    void setUp() {
        folder = Folder.builder().folderId(1L).path("/photos").build();
        asset = Asset.builder().assetId(1L).folder(folder).fileName("photo.jpg").build();
    }

    @Test
    void execute_validCrop_producesImageOfExactFormatTargetDimensions() throws Exception {
        byte[] imageBytes = createTestJpeg(400, 400);
        Asset saved = Asset.builder().assetId(2L).folder(folder).fileName("photo_INSTAGRAM_POST.jpg").build();

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(storagePort.readFileBytes("/photos/photo.jpg")).thenReturn(imageBytes);
        when(catalogFolderPort.createAsset("/photos", "photo_INSTAGRAM_POST.jpg")).thenReturn(saved);

        AtomicReference<byte[]> capturedBytes = new AtomicReference<>();
        doAnswer(invocation -> {
            String tempPath = invocation.getArgument(0);
            capturedBytes.set(Files.readAllBytes(Path.of(tempPath)));
            return null;
        }).when(storagePort).copyFile(any(), any());

        sut.execute(1L, new CropRegion("INSTAGRAM_POST", 0, 0, 400, 400));

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(capturedBytes.get()));
        assertThat(result.getWidth()).isEqualTo(1080);
        assertThat(result.getHeight()).isEqualTo(1080);
    }

    @Test
    void execute_validCrop_savedFileNameIncludesFormatKeySuffix() throws Exception {
        byte[] imageBytes = createTestJpeg(200, 200);
        Asset saved = Asset.builder().assetId(2L).folder(folder).fileName("photo_TWITTER_HEADER.jpg").build();

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(storagePort.readFileBytes(any())).thenReturn(imageBytes);
        when(catalogFolderPort.createAsset(any(), any())).thenReturn(saved);

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);

        sut.execute(1L, new CropRegion("TWITTER_HEADER", 0, 0, 150, 50));

        verify(storagePort).copyFile(any(), destCaptor.capture());
        assertThat(destCaptor.getValue()).endsWith("_TWITTER_HEADER.jpg");
    }

    @Test
    void execute_outputAssetAlreadyExists_returnsExistingAssetWithoutCreatingDuplicate() throws Exception {
        byte[] imageBytes = createTestJpeg(200, 200);
        Asset existing = Asset.builder().assetId(99L).folder(folder).fileName("photo_INSTAGRAM_POST.jpg").build();

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(storagePort.readFileBytes(any())).thenReturn(imageBytes);
        when(assetRepository.findByFolderAndFileName(eq(folder), eq("photo_INSTAGRAM_POST.jpg")))
                .thenReturn(Optional.of(existing));

        Asset result = sut.execute(1L, new CropRegion("INSTAGRAM_POST", 0, 0, 200, 200));

        assertThat(result.getAssetId()).isEqualTo(99L);
        verify(catalogFolderPort, never()).createAsset(any(), any());
    }

    @Test
    void execute_outOfBoundsCoordinates_throwsIllegalArgumentException() throws Exception {
        byte[] imageBytes = createTestJpeg(100, 100);

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(storagePort.readFileBytes(any())).thenReturn(imageBytes);

        CropRegion request = new CropRegion("INSTAGRAM_POST", 50, 50, 100, 100);

        assertThatThrownBy(() -> sut.execute(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out of bounds");
    }

    private byte[] createTestJpeg(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        return baos.toByteArray();
    }
}
