package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.infrastructure.web.dto.shared.SyncDirectoryPairDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SyncWebMapperImplTest {

    private final SyncWebMapper sut = new SyncWebMapperImpl();

    @Test
    void toDto_mapsAllFields() {
        SyncDirectoriesDefinition domain = SyncDirectoriesDefinition.builder()
                .id(1L)
                .sourceDirectory("/src")
                .destinationDirectory("/dst")
                .includeSubFolders(true)
                .deleteAssetsNotInSource(true)
                .order(4)
                .build();

        SyncDirectoryPairDto result = sut.toDto(domain);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sourceDirectory()).isEqualTo("/src");
        assertThat(result.destinationDirectory()).isEqualTo("/dst");
        assertThat(result.includeSubFolders()).isTrue();
        assertThat(result.deleteAssetsNotInSource()).isTrue();
        assertThat(result.order()).isEqualTo(4);
    }

    @Test
    void toDto_nullDefinition_returnsNull() {
        assertThat(sut.toDto(null)).isNull();
    }

    @Test
    void toDomain_mapsAllFields() {
        SyncDirectoryPairDto dto = new SyncDirectoryPairDto(2L, "/a", "/b", false, false, 5);

        SyncDirectoriesDefinition result = sut.toDomain(dto);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getSourceDirectory()).isEqualTo("/a");
        assertThat(result.getDestinationDirectory()).isEqualTo("/b");
        assertThat(result.isIncludeSubFolders()).isFalse();
        assertThat(result.isDeleteAssetsNotInSource()).isFalse();
        assertThat(result.getOrder()).isEqualTo(5);
    }

    @Test
    void toDomain_nullDto_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void toDtoList_mapsEachElement() {
        SyncDirectoriesDefinition domain = SyncDirectoriesDefinition.builder()
                .id(1L).sourceDirectory("/src").destinationDirectory("/dst").order(1).build();

        List<SyncDirectoryPairDto> result = sut.toDtoList(List.of(domain));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void toDtoList_nullList_returnsNull() {
        assertThat(sut.toDtoList(null)).isNull();
    }

    @Test
    void toDomainList_mapsEachElement() {
        SyncDirectoryPairDto dto = new SyncDirectoryPairDto(1L, "/a", "/b", true, true, 1);

        List<SyncDirectoriesDefinition> result = sut.toDomainList(List.of(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void toDomainList_nullList_returnsNull() {
        assertThat(sut.toDomainList(null)).isNull();
    }
}
