package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.dto.UploadProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.UploadStage;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetThumbnailProcessorTest {

    @Mock AssetRepository assetRepository;
    @Mock StoragePort storagePort;
    @Mock ThumbnailPort thumbnailPort;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks AssetThumbnailProcessor sut;

    @Test
    void onAssetUploaded_generatesAndSavesThumbnail() throws Exception {
        AssetUploadedEvent event = new AssetUploadedEvent(1L, "/photos/img.jpg", "/photos", "img.jpg");
        Asset asset = new Asset();
        asset.setAssetId(1L);
        byte[] thumbnailBytes = new byte[]{1, 2, 3};

        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(storagePort.generateThumbnail("/photos/img.jpg", 200, 150)).thenReturn(thumbnailBytes);
        when(assetRepository.completeIfAllStagesFinished(1L)).thenReturn(false);

        sut.onAssetUploaded(event);

        verify(thumbnailPort).saveThumbnail("1.bin", thumbnailBytes);
        verify(assetRepository).updateThumbnail(eq(1L), any(), any());
        verify(assetRepository, never()).save(any());

        ArgumentCaptor<UploadProgressMessage> captor = ArgumentCaptor.forClass(UploadProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.upload.progress"), eq("1"), captor.capture());
        assertThat(captor.getValue().stage()).isEqualTo(UploadStage.THUMBNAIL);
    }

    @Test
    void onAssetUploaded_assetNotFound_throwsWithoutGeneratingThumbnail() {
        AssetUploadedEvent event = new AssetUploadedEvent(99L, "/photos/img.jpg", "/photos", "img.jpg");
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.onAssetUploaded(event)).isInstanceOf(RuntimeException.class);

        verify(thumbnailPort, never()).saveThumbnail(any(), any());
    }
}
