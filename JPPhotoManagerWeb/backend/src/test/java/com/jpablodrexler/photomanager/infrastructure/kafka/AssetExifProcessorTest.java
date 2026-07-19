package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetUploadedEvent;
import com.jpablodrexler.photomanager.application.dto.UploadProgressMessage;
import com.jpablodrexler.photomanager.domain.enums.UploadStage;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.ExifMetadata;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
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
class AssetExifProcessorTest {

    @Mock AssetRepository assetRepository;
    @Mock AssetExifRepository assetExifRepository;
    @Mock StoragePort storagePort;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks AssetExifProcessor sut;

    @Test
    void onAssetUploaded_extractsExifAndUpsertsIntoExifRepository() {
        AssetUploadedEvent event = new AssetUploadedEvent(1L, "/photos/img.jpg", "/photos", "img.jpg");
        ExifMetadata exif = new ExifMetadata("Canon", "EOS 90D", "50mm", "1/200", 2.8, 400, 50.0,
                null, 6000, 4000, 40.0, -74.0, null);

        when(assetRepository.existsById(1L)).thenReturn(true);
        when(storagePort.getExifMetadata("/photos/img.jpg")).thenReturn(exif);
        when(assetExifRepository.findByAssetId(1L)).thenReturn(Optional.empty());
        when(assetRepository.completeIfAllStagesFinished(1L)).thenReturn(false);

        sut.onAssetUploaded(event);

        ArgumentCaptor<AssetExif> exifCaptor = ArgumentCaptor.forClass(AssetExif.class);
        verify(assetExifRepository).save(exifCaptor.capture());
        assertThat(exifCaptor.getValue().getCameraMake()).isEqualTo("Canon");
        assertThat(exifCaptor.getValue().getAssetId()).isEqualTo(1L);

        verify(assetRepository).updateExifCompletedAt(eq(1L), any());
        verify(assetRepository, never()).save(any());

        ArgumentCaptor<UploadProgressMessage> progressCaptor = ArgumentCaptor.forClass(UploadProgressMessage.class);
        verify(kafkaTemplate).send(eq("job.upload.progress"), eq("1"), progressCaptor.capture());
        assertThat(progressCaptor.getValue().stage()).isEqualTo(UploadStage.EXIF);
        assertThat(progressCaptor.getValue().done()).isFalse();
    }

    @Test
    void onAssetUploaded_assetNotFound_throwsWithoutUpsertingExif() {
        AssetUploadedEvent event = new AssetUploadedEvent(99L, "/photos/img.jpg", "/photos", "img.jpg");
        when(assetRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> sut.onAssetUploaded(event)).isInstanceOf(RuntimeException.class);

        verify(assetExifRepository, never()).save(any());
    }
}
