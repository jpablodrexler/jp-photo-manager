package com.jpablodrexler.photomanager.infrastructure.migration;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetExifEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetExifEntityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetExifMongoMigrationRunnerTest {

    @Mock JpaAssetExifRepository jpaAssetExifRepository;
    @Mock AssetExifEntityMapper entityMapper;
    @Mock AssetExifRepository assetExifRepository;
    @Mock MongoTemplate mongoTemplate;
    @InjectMocks AssetExifMongoMigrationRunner sut;

    @Test
    void run_markerAbsent_migratesAllRowsAndWritesMarker() throws Exception {
        when(mongoTemplate.exists(any(Query.class), eq(AssetExifMongoMigrationRunner.MARKER_COLLECTION)))
                .thenReturn(false);

        AssetExifEntity entity1 = new AssetExifEntity();
        entity1.setAssetId(1L);
        AssetExifEntity entity2 = new AssetExifEntity();
        entity2.setAssetId(2L);
        AssetExif domain1 = AssetExif.builder().assetId(1L).build();
        AssetExif domain2 = AssetExif.builder().assetId(2L).build();

        when(jpaAssetExifRepository.count()).thenReturn(2L);
        Page<AssetExifEntity> page = new PageImpl<>(List.of(entity1, entity2), PageRequest.of(0, 500), 2);
        when(jpaAssetExifRepository.findAll(PageRequest.of(0, 500))).thenReturn(page);
        when(entityMapper.toDomain(entity1)).thenReturn(domain1);
        when(entityMapper.toDomain(entity2)).thenReturn(domain2);

        sut.run();

        verify(assetExifRepository).save(domain1);
        verify(assetExifRepository).save(domain2);
        ArgumentCaptor<Object> markerCaptor = ArgumentCaptor.forClass(Object.class);
        verify(mongoTemplate).save(markerCaptor.capture(), eq(AssetExifMongoMigrationRunner.MARKER_COLLECTION));
    }

    @Test
    void run_markerPresent_skipsMigration() throws Exception {
        when(mongoTemplate.exists(any(Query.class), eq(AssetExifMongoMigrationRunner.MARKER_COLLECTION)))
                .thenReturn(true);

        sut.run();

        verify(jpaAssetExifRepository, never()).findAll(any(PageRequest.class));
        verify(assetExifRepository, never()).save(any());
        verify(mongoTemplate, never()).save(any(), anyString());
    }

    @Test
    void run_resumedAfterPartialCrash_reUpsertsWithoutDuplication() throws Exception {
        when(mongoTemplate.exists(any(Query.class), eq(AssetExifMongoMigrationRunner.MARKER_COLLECTION)))
                .thenReturn(false);

        AssetExifEntity entity = new AssetExifEntity();
        entity.setAssetId(1L);
        AssetExif domain = AssetExif.builder().assetId(1L).build();

        when(jpaAssetExifRepository.count()).thenReturn(1L);
        Page<AssetExifEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 500), 1);
        when(jpaAssetExifRepository.findAll(PageRequest.of(0, 500))).thenReturn(page);
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        // Simulate a resumed run re-processing the same already-migrated row.
        sut.run();
        sut.run();

        // save() is called once per run for the same row; the port's upsert-by-assetId semantics
        // (AssetExifRepositoryImpl.save) guarantee no duplicate document is created.
        verify(assetExifRepository, org.mockito.Mockito.times(2)).save(domain);
    }
}
