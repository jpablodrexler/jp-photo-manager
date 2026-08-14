package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.RecentTargetPath;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.RecentTargetPathEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecentTargetPathEntityMapperImplTest {

    private final RecentTargetPathEntityMapper sut = new RecentTargetPathEntityMapperImpl();

    @Test
    void toDomain_mapsFields() {
        RecentTargetPathEntity entity = new RecentTargetPathEntity("/exports/2024");
        entity.setId(5L);

        RecentTargetPath result = sut.toDomain(entity);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getPath()).isEqualTo("/exports/2024");
    }

    @Test
    void toDomain_nullEntity_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void toEntity_mapsFields() {
        RecentTargetPath domain = RecentTargetPath.builder().id(6L).path("/exports/2025").build();

        RecentTargetPathEntity result = sut.toEntity(domain);

        assertThat(result.getId()).isEqualTo(6L);
        assertThat(result.getPath()).isEqualTo("/exports/2025");
    }

    @Test
    void toEntity_nullDomain_returnsNull() {
        assertThat(sut.toEntity(null)).isNull();
    }
}
