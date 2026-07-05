package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.infrastructure.persistence.document.AssetExifDocument;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetExifDocumentMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.mongo.MongoAssetExifRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetExifRepositoryImplTest {

    @Mock MongoAssetExifRepository mongoRepository;
    @Mock AssetExifDocumentMapper mapper;
    @InjectMocks AssetExifRepositoryImpl sut;

    @Test
    void findByAssetId_present_returnsMappedDomain() {
        AssetExifDocument document = new AssetExifDocument();
        AssetExif domain = AssetExif.builder().assetId(1L).cameraMake("Canon").build();
        when(mongoRepository.findByAssetId(1L)).thenReturn(Optional.of(document));
        when(mapper.toDomain(document)).thenReturn(domain);

        Optional<AssetExif> result = sut.findByAssetId(1L);

        assertThat(result).contains(domain);
    }

    @Test
    void findByAssetId_absent_returnsEmpty() {
        when(mongoRepository.findByAssetId(99L)).thenReturn(Optional.empty());

        assertThat(sut.findByAssetId(99L)).isEmpty();
    }

    @Test
    void save_existingDocument_updatesInPlace() {
        AssetExif exif = AssetExif.builder()
                .assetId(1L)
                .cameraMake("Sony")
                .cameraModel("A7")
                .isoSpeed(400)
                .build();
        AssetExifDocument existing = new AssetExifDocument();
        existing.setId("existing-id");
        AssetExifDocument mapped = new AssetExifDocument();
        AssetExifDocument saved = new AssetExifDocument();
        when(mongoRepository.findByAssetId(1L)).thenReturn(Optional.of(existing));
        when(mapper.toDocument(exif)).thenReturn(mapped);
        when(mongoRepository.save(mapped)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(exif);

        AssetExif result = sut.save(exif);

        assertThat(result).isEqualTo(exif);
        assertThat(mapped.getId()).isEqualTo("existing-id");
        ArgumentCaptor<AssetExifDocument> captor = ArgumentCaptor.forClass(AssetExifDocument.class);
        verify(mongoRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("existing-id");
    }

    @Test
    void save_newDocument_insertsWithoutId() {
        AssetExif exif = AssetExif.builder()
                .assetId(2L)
                .cameraMake("Nikon")
                .build();
        AssetExifDocument mapped = new AssetExifDocument();
        AssetExifDocument saved = new AssetExifDocument();
        when(mongoRepository.findByAssetId(2L)).thenReturn(Optional.empty());
        when(mapper.toDocument(exif)).thenReturn(mapped);
        when(mongoRepository.save(mapped)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(exif);

        AssetExif result = sut.save(exif);

        assertThat(result).isEqualTo(exif);
        assertThat(mapped.getId()).isNull();
        verify(mongoRepository).save(mapped);
    }

    @Test
    void deleteByAssetId_delegatesToMongoRepository() {
        sut.deleteByAssetId(3L);

        verify(mongoRepository).deleteByAssetId(3L);
    }
}
