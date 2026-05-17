package com.jpablodrexler.photomanager.application.usecase.sync;

import com.jpablodrexler.photomanager.domain.model.SyncDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.out.SyncConfigRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncConfigUseCasesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class SaveSyncConfigUseCaseImplTest {

        @Mock SyncConfigRepository syncConfigRepository;
        @InjectMocks SaveSyncConfigUseCaseImpl sut;

        @Test
        void execute_deletesOldAndSavesWithAssignedOrder() {
            SyncDirectoriesDefinition d1 = SyncDirectoriesDefinition.builder()
                    .id(10L).sourceDirectory("/a").destinationDirectory("/b").build();
            SyncDirectoriesDefinition d2 = SyncDirectoriesDefinition.builder()
                    .id(20L).sourceDirectory("/c").destinationDirectory("/d").build();

            sut.execute(List.of(d1, d2));

            verify(syncConfigRepository).deleteAll();
            verify(syncConfigRepository).saveAll(List.of(d1, d2));
            assertThat(d1.getId()).isNull();
            assertThat(d1.getOrder()).isZero();
            assertThat(d2.getId()).isNull();
            assertThat(d2.getOrder()).isEqualTo(1);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetSyncConfigUseCaseImplTest {

        @Mock SyncConfigRepository syncConfigRepository;
        @InjectMocks GetSyncConfigUseCaseImpl sut;

        @Test
        void execute_returnsDefinitions() {
            List<SyncDirectoriesDefinition> expected = List.of(
                    SyncDirectoriesDefinition.builder().sourceDirectory("/src").build());
            when(syncConfigRepository.findAllOrderByOrder()).thenReturn(expected);

            List<SyncDirectoriesDefinition> result = sut.execute();

            assertThat(result).isEqualTo(expected);
        }
    }
}
