package com.jpablodrexler.photomanager.application.usecase.convert;

import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.out.ConvertConfigRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConvertUseCasesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ConvertAssetsUseCaseImplTest {

        @Mock ConvertConfigRepository convertConfigRepository;
        @Mock StoragePort storagePort;
        @InjectMocks ConvertAssetsUseCaseImpl sut;

        @Test
        void execute_noDefinitions_returnsEmptyList() throws Exception {
            when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of());

            List<ConvertAssetsResult> result = sut.execute(null).get();

            assertThat(result).isEmpty();
        }

        @Test
        void execute_sourceNotExist_returnsFailureResult() throws Exception {
            ConvertDirectoriesDefinition def = buildDef("/src", "/dest");
            when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
            when(storagePort.directoryExists("/src")).thenReturn(false);

            List<ConvertAssetsResult> result = sut.execute(null).get();

            assertThat(result.get(0).isSuccess()).isFalse();
            assertThat(result.get(0).getMessage()).contains("does not exist");
        }

        @Test
        void execute_convertsPngFiles() throws Exception {
            ConvertDirectoriesDefinition def = buildDef("/src", "/dest");
            when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
            when(storagePort.directoryExists(anyString())).thenReturn(true);
            when(storagePort.listFiles("/src")).thenReturn(List.of("/src/photo.png"));

            List<ConvertAssetsResult> result = sut.execute(null).get();

            assertThat(result.get(0).getConvertedCount()).isEqualTo(1);
            verify(storagePort).convertPngToJpeg("/src/photo.png", "/dest/photo.jpg");
        }

        @Test
        void execute_skipsNonPngFiles() throws Exception {
            ConvertDirectoriesDefinition def = buildDef("/src", "/dest");
            when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
            when(storagePort.directoryExists(anyString())).thenReturn(true);
            when(storagePort.listFiles("/src")).thenReturn(List.of("/src/photo.jpg"));

            List<ConvertAssetsResult> result = sut.execute(null).get();

            assertThat(result.get(0).getConvertedCount()).isZero();
            verify(storagePort, never()).convertPngToJpeg(any(), any());
        }

        @Test
        void execute_convertFails_incrementsFailedCount() throws Exception {
            ConvertDirectoriesDefinition def = buildDef("/src", "/dest");
            when(convertConfigRepository.findAllOrderByOrder()).thenReturn(List.of(def));
            when(storagePort.directoryExists(anyString())).thenReturn(true);
            when(storagePort.listFiles("/src")).thenReturn(List.of("/src/photo.png"));
            org.mockito.Mockito.doThrow(new IOException("convert error"))
                    .when(storagePort).convertPngToJpeg(anyString(), anyString());

            List<ConvertAssetsResult> result = sut.execute(null).get();

            assertThat(result.get(0).getFailedCount()).isEqualTo(1);
        }

        private ConvertDirectoriesDefinition buildDef(String src, String dest) {
            return ConvertDirectoriesDefinition.builder()
                    .sourceDirectory(src)
                    .destinationDirectory(dest)
                    .build();
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class SaveConvertConfigUseCaseImplTest {

        @Mock ConvertConfigRepository convertConfigRepository;
        @InjectMocks SaveConvertConfigUseCaseImpl sut;

        @Test
        void execute_deletesOldAndSavesWithAssignedOrder() {
            ConvertDirectoriesDefinition d1 = ConvertDirectoriesDefinition.builder()
                    .id(99L).sourceDirectory("/a").destinationDirectory("/b").build();
            ConvertDirectoriesDefinition d2 = ConvertDirectoriesDefinition.builder()
                    .id(100L).sourceDirectory("/c").destinationDirectory("/d").build();

            sut.execute(List.of(d1, d2));

            verify(convertConfigRepository).deleteAll();
            verify(convertConfigRepository).saveAll(List.of(d1, d2));
            assertThat(d1.getId()).isNull();
            assertThat(d1.getOrder()).isZero();
            assertThat(d2.getId()).isNull();
            assertThat(d2.getOrder()).isEqualTo(1);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetConvertConfigUseCaseImplTest {

        @Mock ConvertConfigRepository convertConfigRepository;
        @InjectMocks GetConvertConfigUseCaseImpl sut;

        @Test
        void execute_returnsDefinitions() {
            List<ConvertDirectoriesDefinition> expected = List.of(
                    ConvertDirectoriesDefinition.builder().sourceDirectory("/x").build());
            when(convertConfigRepository.findAllOrderByOrder()).thenReturn(expected);

            List<ConvertDirectoriesDefinition> result = sut.execute();

            assertThat(result).isEqualTo(expected);
        }
    }
}
