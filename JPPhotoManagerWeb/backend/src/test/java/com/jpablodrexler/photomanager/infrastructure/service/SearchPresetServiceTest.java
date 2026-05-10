package com.jpablodrexler.photomanager.infrastructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.api.exception.SearchPresetNotFoundException;
import com.jpablodrexler.photomanager.application.dto.FilterPreset;
import com.jpablodrexler.photomanager.domain.entity.SearchPreset;
import com.jpablodrexler.photomanager.domain.entity.User;
import com.jpablodrexler.photomanager.domain.repository.SearchPresetRepository;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchPresetServiceTest {

    @Mock
    SearchPresetRepository searchPresetRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    SearchPresetServiceImpl sut;

    @Test
    void createPreset_validInput_serializesFilterAndSaves() throws JsonProcessingException {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        FilterPreset filter = new FilterPreset("vacation", null, null, 3);
        SearchPreset saved = new SearchPreset();
        saved.setPresetId(1L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(filter)).thenReturn("{\"search\":\"vacation\",\"minRating\":3}");
        when(searchPresetRepository.save(any())).thenReturn(saved);

        SearchPreset result = sut.createPreset(userId, "Vacation 3-star", filter);

        verify(objectMapper).writeValueAsString(filter);
        ArgumentCaptor<SearchPreset> captor = ArgumentCaptor.forClass(SearchPreset.class);
        verify(searchPresetRepository).save(captor.capture());
        SearchPreset persisted = captor.getValue();
        assertThat(persisted.getName()).isEqualTo("Vacation 3-star");
        assertThat(persisted.getFilterJson()).isEqualTo("{\"search\":\"vacation\",\"minRating\":3}");
        assertThat(persisted.getUser()).isSameAs(user);
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(result).isSameAs(saved);
    }

    @Test
    void deletePreset_existingPreset_deletesIt() {
        UUID userId = UUID.randomUUID();
        SearchPreset preset = new SearchPreset();
        preset.setPresetId(7L);
        when(searchPresetRepository.findByPresetIdAndUser_Id(7L, userId)).thenReturn(Optional.of(preset));

        sut.deletePreset(userId, 7L);

        verify(searchPresetRepository).delete(preset);
    }

    @Test
    void deletePreset_unknownPreset_throwsSearchPresetNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(searchPresetRepository.findByPresetIdAndUser_Id(999L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.deletePreset(userId, 999L))
                .isInstanceOf(SearchPresetNotFoundException.class)
                .hasMessageContaining("999");
    }
}
