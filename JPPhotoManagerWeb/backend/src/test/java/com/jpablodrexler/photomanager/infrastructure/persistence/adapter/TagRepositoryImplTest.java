package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.TagEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaTagRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.TagEntityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagRepositoryImplTest {

    @Mock JpaTagRepository jpa;
    @Mock TagEntityMapper tagMapper;
    @InjectMocks TagRepositoryImpl sut;

    @Test
    void findByName_present_returnsMappedDomain() {
        TagEntity entity = new TagEntity();
        Tag domain = Tag.builder().tagId(1L).name("vacation").build();
        when(jpa.findByName("vacation")).thenReturn(Optional.of(entity));
        when(tagMapper.toDomain(entity)).thenReturn(domain);

        assertThat(sut.findByName("vacation")).contains(domain);
    }

    @Test
    void findByName_absent_returnsEmpty() {
        when(jpa.findByName("missing")).thenReturn(Optional.empty());

        assertThat(sut.findByName("missing")).isEmpty();
    }

    @Test
    void findByNameContaining_returnsMappedList() {
        TagEntity entity = new TagEntity();
        Tag domain = Tag.builder().tagId(1L).name("vacation").build();
        when(jpa.findByNameContainingIgnoreCaseOrderByName("vac", PageRequest.of(0, 10)))
                .thenReturn(List.of(entity));
        when(tagMapper.toDomain(entity)).thenReturn(domain);

        List<Tag> result = sut.findByNameContaining("vac", 10);

        assertThat(result).containsExactly(domain);
    }

    @Test
    void save_mapsAndPersists() {
        Tag tag = Tag.builder().tagId(1L).name("family").build();
        TagEntity entity = new TagEntity();
        when(tagMapper.toEntity(tag)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(entity);
        when(tagMapper.toDomain(entity)).thenReturn(tag);

        assertThat(sut.save(tag)).isEqualTo(tag);
    }

    @Test
    void deleteById_delegatesToJpa() {
        sut.deleteById(5L);
        verify(jpa).deleteById(5L);
    }

    @Test
    void isUsedByOtherAssets_delegatesToJpa() {
        when(jpa.isUsedByOtherAssets(1L, 2L)).thenReturn(true);

        assertThat(sut.isUsedByOtherAssets(1L, 2L)).isTrue();
    }
}
