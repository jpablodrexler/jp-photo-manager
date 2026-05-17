package com.jpablodrexler.photomanager.application.usecase.home;

import com.jpablodrexler.photomanager.application.dto.HomeStats;
import com.jpablodrexler.photomanager.domain.model.CatalogRunState;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.CatalogStateRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHomeStatsUseCaseImplTest {

    @Mock FolderRepository folderRepository;
    @Mock AssetRepository assetRepository;
    @Mock CatalogStateRepository catalogStateRepository;
    @InjectMocks GetHomeStatsUseCaseImpl sut;

    @Test
    void execute_withLastCompleted_returnsStatsWithLastCatalogDate() {
        Instant completedAt = Instant.now();
        CatalogRunState state = CatalogRunState.builder().lastCompletedAt(completedAt).build();
        when(folderRepository.count()).thenReturn(5L);
        when(assetRepository.count()).thenReturn(200L);
        when(catalogStateRepository.find()).thenReturn(Optional.of(state));

        HomeStats result = sut.execute();

        assertThat(result.folderCount()).isEqualTo(5L);
        assertThat(result.assetCount()).isEqualTo(200L);
        assertThat(result.lastCatalogCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void execute_withoutLastCompleted_returnsStatsWithNullLastCatalogDate() {
        when(folderRepository.count()).thenReturn(0L);
        when(assetRepository.count()).thenReturn(0L);
        when(catalogStateRepository.find()).thenReturn(Optional.empty());

        HomeStats result = sut.execute();

        assertThat(result.lastCatalogCompletedAt()).isNull();
    }
}
