package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.infrastructure.web.dto.shared.ConvertDirectoryPairDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertWebMapperImplTest {

    private final ConvertWebMapper sut = new ConvertWebMapperImpl();

    @Test
    void toDto_mapsAllFields() {
        ConvertDirectoriesDefinition domain = ConvertDirectoriesDefinition.builder()
                .id(1L)
                .sourceDirectory("/src")
                .destinationDirectory("/dst")
                .includeSubFolders(true)
                .deleteAssetsNotInSource(false)
                .order(2)
                .build();

        ConvertDirectoryPairDto result = sut.toDto(domain);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sourceDirectory()).isEqualTo("/src");
        assertThat(result.destinationDirectory()).isEqualTo("/dst");
        assertThat(result.includeSubFolders()).isTrue();
        assertThat(result.deleteAssetsNotInSource()).isFalse();
        assertThat(result.order()).isEqualTo(2);
    }

    @Test
    void toDto_nullDefinition_returnsNull() {
        assertThat(sut.toDto(null)).isNull();
    }

    @Test
    void toDomain_mapsAllFields() {
        ConvertDirectoryPairDto dto = new ConvertDirectoryPairDto(2L, "/a", "/b", false, true, 3);

        ConvertDirectoriesDefinition result = sut.toDomain(dto);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getSourceDirectory()).isEqualTo("/a");
        assertThat(result.getDestinationDirectory()).isEqualTo("/b");
        assertThat(result.isIncludeSubFolders()).isFalse();
        assertThat(result.isDeleteAssetsNotInSource()).isTrue();
        assertThat(result.getOrder()).isEqualTo(3);
    }

    @Test
    void toDomain_nullDto_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void toDtoList_mapsEachElement() {
        ConvertDirectoriesDefinition domain = ConvertDirectoriesDefinition.builder()
                .id(1L).sourceDirectory("/src").destinationDirectory("/dst").order(1).build();

        List<ConvertDirectoryPairDto> result = sut.toDtoList(List.of(domain));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void toDtoList_nullList_returnsNull() {
        assertThat(sut.toDtoList(null)).isNull();
    }

    @Test
    void toDomainList_mapsEachElement() {
        ConvertDirectoryPairDto dto = new ConvertDirectoryPairDto(1L, "/a", "/b", true, false, 1);

        List<ConvertDirectoriesDefinition> result = sut.toDomainList(List.of(dto));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void toDomainList_nullList_returnsNull() {
        assertThat(sut.toDomainList(null)).isNull();
    }
}
