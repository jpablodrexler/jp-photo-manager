package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetExifEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetExifEntityMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetExifRepositoryImplTest {

    @Mock JpaAssetExifRepository jpa;
    @Mock AssetExifEntityMapper mapper;
    @Mock EntityManager entityManager;
    @InjectMocks AssetExifRepositoryImpl sut;

    @Test
    void findByAssetId_present_returnsMappedDomain() {
        AssetExifEntity entity = new AssetExifEntity();
        AssetExif domain = AssetExif.builder().assetId(1L).cameraMake("Canon").build();
        when(jpa.findByAssetAssetId(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<AssetExif> result = sut.findByAssetId(1L);

        assertThat(result).contains(domain);
    }

    @Test
    void findByAssetId_absent_returnsEmpty() {
        when(jpa.findByAssetAssetId(99L)).thenReturn(Optional.empty());

        assertThat(sut.findByAssetId(99L)).isEmpty();
    }

    @Test
    void save_existingEntity_updatesFieldsAndSaves() {
        AssetExif exif = AssetExif.builder()
                .assetId(1L)
                .cameraMake("Sony")
                .cameraModel("A7")
                .isoSpeed(400)
                .build();
        AssetExifEntity existing = new AssetExifEntity();
        AssetExifEntity saved = new AssetExifEntity();
        AssetEntity assetRef = new AssetEntity();
        when(jpa.findByAssetAssetId(1L)).thenReturn(Optional.of(existing));
        when(entityManager.getReference(AssetEntity.class, 1L)).thenReturn(assetRef);
        when(jpa.save(existing)).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(exif);

        AssetExif result = sut.save(exif);

        assertThat(result).isEqualTo(exif);
        assertThat(existing.getCameraMake()).isEqualTo("Sony");
        assertThat(existing.getCameraModel()).isEqualTo("A7");
        assertThat(existing.getIsoSpeed()).isEqualTo(400);
        verify(jpa).save(existing);
    }

    @Test
    void save_newEntity_createsAndSaves() {
        AssetExif exif = AssetExif.builder()
                .assetId(2L)
                .cameraMake("Nikon")
                .build();
        AssetEntity assetRef = new AssetEntity();
        AssetExifEntity saved = new AssetExifEntity();
        when(jpa.findByAssetAssetId(2L)).thenReturn(Optional.empty());
        when(entityManager.getReference(AssetEntity.class, 2L)).thenReturn(assetRef);
        when(jpa.save(any(AssetExifEntity.class))).thenReturn(saved);
        when(mapper.toDomain(saved)).thenReturn(exif);

        AssetExif result = sut.save(exif);

        assertThat(result).isEqualTo(exif);
        verify(jpa).save(any(AssetExifEntity.class));
    }

    @Test
    void deleteByAssetId_delegatesToJpa() {
        sut.deleteByAssetId(3L);
        verify(jpa).deleteByAssetAssetId(3L);
    }
}
