package com.jpablodrexler.photomanager.application.usecase.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.FilterPreset;
import com.jpablodrexler.photomanager.application.exception.SearchPresetNotFoundException;
import com.jpablodrexler.photomanager.domain.model.SearchPreset;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.SearchPresetRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchPresetUseCasesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class CreateSearchPresetUseCaseImplTest {

        @Mock SearchPresetRepository searchPresetRepository;
        @Mock UserRepository userRepository;
        @Spy ObjectMapper objectMapper;
        @InjectMocks CreateSearchPresetUseCaseImpl sut;

        @Test
        void execute_userNotFound_throwsNoSuchElementException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(userId, "My Preset", new FilterPreset("cats", null, null, null)))
                    .isInstanceOf(java.util.NoSuchElementException.class);
            verify(searchPresetRepository, never()).save(any());
        }

        @Test
        void execute_userFound_savesPreset() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).username("alice").build();
            SearchPreset saved = SearchPreset.builder().presetId(1L).userId(userId).name("My Preset")
                    .filterJson("{}").createdAt(Instant.now()).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(searchPresetRepository.save(any())).thenReturn(saved);

            SearchPreset result = sut.execute(userId, "My Preset", new FilterPreset("cats", null, null, null));

            assertThat(result.getPresetId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("My Preset");
            verify(searchPresetRepository).save(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class DeleteSearchPresetUseCaseImplTest {

        @Mock SearchPresetRepository searchPresetRepository;
        @InjectMocks DeleteSearchPresetUseCaseImpl sut;

        @Test
        void execute_notFound_throwsSearchPresetNotFoundException() {
            UUID userId = UUID.randomUUID();
            when(searchPresetRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(99L, userId))
                    .isInstanceOf(SearchPresetNotFoundException.class);
            verify(searchPresetRepository, never()).deleteById(any());
        }

        @Test
        void execute_found_deletesById() {
            UUID userId = UUID.randomUUID();
            SearchPreset preset = SearchPreset.builder().presetId(1L).userId(userId).build();
            when(searchPresetRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(preset));

            sut.execute(1L, userId);

            verify(searchPresetRepository).deleteById(1L);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetSearchPresetsUseCaseImplTest {

        @Mock SearchPresetRepository searchPresetRepository;
        @InjectMocks GetSearchPresetsUseCaseImpl sut;

        @Test
        void execute_returnsPresetsForUser() {
            UUID userId = UUID.randomUUID();
            List<SearchPreset> presets = List.of(SearchPreset.builder().presetId(1L).build());
            when(searchPresetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(presets);

            List<SearchPreset> result = sut.execute(userId);

            assertThat(result).isEqualTo(presets);
        }
    }
}
