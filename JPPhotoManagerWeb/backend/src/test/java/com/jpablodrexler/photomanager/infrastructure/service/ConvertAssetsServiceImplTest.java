package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.domain.entity.ConvertAssetsDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.repository.ConvertAssetsConfigRepository;
import com.jpablodrexler.photomanager.domain.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConvertAssetsServiceImplTest {

    @Mock
    ConvertAssetsConfigRepository configRepository;

    @Mock
    StorageService storageService;

    @InjectMocks
    ConvertAssetsServiceImpl sut;

    @TempDir
    Path tempDir;

    @Test
    void executeAsync_noDefinitions_returnsEmptyList() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of());

        List<ConvertAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results).isEmpty();
    }

    @Test
    void executeAsync_sourceDirectoryMissing_resultMarkedFailed() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest")));
        when(storageService.directoryExists("/source")).thenReturn(false);

        List<ConvertAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results.get(0).isSuccess()).isFalse();
        assertThat(results.get(0).getMessage()).contains("/source");
    }

    @Test
    void executeAsync_destinationDirectoryMissing_createsDestinationDirectory() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", "/dest")));
        when(storageService.directoryExists("/source")).thenReturn(true);
        when(storageService.directoryExists("/dest")).thenReturn(false);
        when(storageService.listFiles(any())).thenReturn(List.of());

        sut.executeAsync(null).get();

        verify(storageService).createDirectory("/dest");
    }

    @Test
    void executeAsync_pngFile_convertsToJpegAndIncrementsConvertedCount() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", tempDir.toString())));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/photo.png"));
        when(storageService.loadImage("/source/photo.png"))
                .thenReturn(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));

        List<ConvertAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results.get(0).getConvertedCount()).isEqualTo(1);
        assertThat(results.get(0).isSuccess()).isTrue();
    }

    @Test
    void executeAsync_nonPngFile_isSkippedAndNotCounted() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", tempDir.toString())));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/photo.jpg"));

        List<ConvertAssetsResult> results = sut.executeAsync(null).get();

        verify(storageService, never()).loadImage(any());
        assertThat(results.get(0).getConvertedCount()).isZero();
        assertThat(results.get(0).isSuccess()).isTrue();
    }

    @Test
    void executeAsync_pngConversionFailure_incrementsFailedCount() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", tempDir.toString())));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/photo.png"));
        when(storageService.loadImage("/source/photo.png")).thenThrow(new IOException("corrupt"));

        List<ConvertAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results.get(0).getFailedCount()).isEqualTo(1);
        assertThat(results.get(0).getConvertedCount()).isZero();
    }

    @Test
    void executeAsync_pngConversionFailure_doesNotMarkResultFailed() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", tempDir.toString())));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/photo.png"));
        when(storageService.loadImage("/source/photo.png")).thenThrow(new IOException("corrupt"));

        List<ConvertAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results.get(0).isSuccess()).isTrue();
    }

    @Test
    void executeAsync_callbackProvided_invokesCallbackOnConversion() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", tempDir.toString())));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/photo.png"));
        when(storageService.loadImage("/source/photo.png"))
                .thenReturn(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));
        List<String> messages = new ArrayList<>();

        sut.executeAsync(messages::add).get();

        assertThat(messages).anyMatch(m -> m.contains("photo.png") && m.contains("photo.jpg"));
    }

    @Test
    void executeAsync_multipleDefinitions_returnsOneResultPerDefinition() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(
                buildDef("/src1", "/dst1"),
                buildDef("/src2", "/dst2")));
        when(storageService.directoryExists(any())).thenReturn(false);

        List<ConvertAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results).hasSize(2);
    }

    @Test
    void executeAsync_upperCasePngExtension_isAlsoConverted() throws Exception {
        when(configRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(buildDef("/source", tempDir.toString())));
        when(storageService.directoryExists(any())).thenReturn(true);
        when(storageService.listFiles("/source")).thenReturn(List.of("/source/PHOTO.PNG"));
        when(storageService.loadImage("/source/PHOTO.PNG"))
                .thenReturn(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB));

        List<ConvertAssetsResult> results = sut.executeAsync(null).get();

        assertThat(results.get(0).getConvertedCount()).isEqualTo(1);
    }

    private ConvertAssetsDirectoriesDefinition buildDef(String source, String dest) {
        ConvertAssetsDirectoriesDefinition def = new ConvertAssetsDirectoriesDefinition();
        def.setSourceDirectory(source);
        def.setDestinationDirectory(dest);
        return def;
    }
}
