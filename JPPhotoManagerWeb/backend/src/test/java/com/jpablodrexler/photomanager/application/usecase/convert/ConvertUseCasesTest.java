package com.jpablodrexler.photomanager.application.usecase.convert;

import com.jpablodrexler.photomanager.domain.model.ConvertDirectoriesDefinition;
import com.jpablodrexler.photomanager.domain.port.out.ConvertConfigRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ConvertUseCasesTest {

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
