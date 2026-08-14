package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.TagEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagEntityMapperImplTest {

    private final TagEntityMapper sut = new TagEntityMapperImpl();

    @Test
    void toDomain_mapsFields() {
        TagEntity entity = new TagEntity();
        entity.setTagId(1L);
        entity.setName("landscape");

        Tag result = sut.toDomain(entity);

        assertThat(result.getTagId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("landscape");
    }

    @Test
    void toDomain_nullEntity_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void toEntity_mapsFields() {
        Tag domain = Tag.builder().tagId(2L).name("portrait").build();

        TagEntity result = sut.toEntity(domain);

        assertThat(result.getTagId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo("portrait");
    }

    @Test
    void toEntity_nullDomain_returnsNull() {
        assertThat(sut.toEntity(null)).isNull();
    }
}
