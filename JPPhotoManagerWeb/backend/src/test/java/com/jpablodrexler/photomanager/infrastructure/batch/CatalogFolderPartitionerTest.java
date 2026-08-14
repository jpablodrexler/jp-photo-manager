package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogFolderPartitionerTest {

    @Mock StoragePort storagePort;
    @InjectMocks CatalogFolderPartitioner sut;

    @Test
    void partition_singleRootNoSubfolders_createsOnePartition() {
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/photos");
        when(storagePort.directoryExists("/photos")).thenReturn(true);
        when(storagePort.listSubDirectories("/photos")).thenReturn(List.of());

        Map<String, ExecutionContext> result = sut.partition(4);

        assertThat(result).hasSize(1);
        assertThat(result.get("partition0").getString("folderPath")).isEqualTo("/photos");
        assertThat(result.get("partition0").getInt("startIndex")).isZero();
    }

    @Test
    void partition_rootWithSubfolders_recursivelyCollectsAll() {
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/photos");
        when(storagePort.directoryExists("/photos")).thenReturn(true);
        when(storagePort.listSubDirectories("/photos")).thenReturn(List.of("/photos/2024"));
        when(storagePort.listSubDirectories("/photos/2024")).thenReturn(List.of("/photos/2024/summer"));
        when(storagePort.listSubDirectories("/photos/2024/summer")).thenReturn(List.of());

        Map<String, ExecutionContext> result = sut.partition(4);

        assertThat(result).hasSize(3);
        List<String> folderPaths = result.values().stream()
                .map(ctx -> ctx.getString("folderPath"))
                .toList();
        assertThat(folderPaths).containsExactly("/photos", "/photos/2024", "/photos/2024/summer");
    }

    @Test
    void partition_multipleRootsSeparatedBySemicolon_trimsAndCollectsEach() {
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/photos ; /music");
        when(storagePort.directoryExists("/photos")).thenReturn(true);
        when(storagePort.directoryExists("/music")).thenReturn(true);
        when(storagePort.listSubDirectories("/photos")).thenReturn(List.of());
        when(storagePort.listSubDirectories("/music")).thenReturn(List.of());

        Map<String, ExecutionContext> result = sut.partition(4);

        assertThat(result).hasSize(2);
        List<String> folderPaths = result.values().stream()
                .map(ctx -> ctx.getString("folderPath"))
                .toList();
        assertThat(folderPaths).containsExactly("/photos", "/music");
    }

    @Test
    void partition_rootDoesNotExist_isSkipped() {
        ReflectionTestUtils.setField(sut, "rootCatalogFolders", "/missing");
        when(storagePort.directoryExists("/missing")).thenReturn(false);

        Map<String, ExecutionContext> result = sut.partition(4);

        assertThat(result).isEmpty();
    }
}
