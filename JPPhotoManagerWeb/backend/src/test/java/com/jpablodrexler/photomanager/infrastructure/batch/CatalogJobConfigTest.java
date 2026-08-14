package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.port.out.AssetAudioRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import com.jpablodrexler.photomanager.infrastructure.service.AudioMetadataService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogJobConfigTest {

    @Mock JobRepository jobRepository;
    @Mock PlatformTransactionManager transactionManager;
    @Mock Step step;
    @Mock CatalogItemWriteListener catalogItemWriteListener;
    @Mock CatalogFolderPartitioner partitioner;
    @Mock CatalogFileItemReader catalogFileItemReader;
    @Mock CatalogAssetItemProcessor catalogAssetItemProcessor;
    @Mock CatalogAssetItemWriter catalogAssetItemWriter;
    @Mock AssetRepository assetRepository;
    @Mock AssetExifRepository assetExifRepository;
    @Mock AssetAudioRepository assetAudioRepository;
    @Mock FolderRepository folderRepository;
    @Mock StoragePort storagePort;
    @Mock ThumbnailPort thumbnailPort;
    @Mock AudioMetadataService audioMetadataService;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    CatalogJobConfig sut;

    @BeforeEach
    void setUp() {
        sut = new CatalogJobConfig();
        ReflectionTestUtils.setField(sut, "gridSize", 4);
        ReflectionTestUtils.setField(sut, "chunkSize", 50);
    }

    @Test
    void catalogJob_buildsJobWithExpectedName() {
        Job job = sut.catalogJob(jobRepository, step, catalogItemWriteListener);

        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("catalogJob");
    }

    @Test
    void catalogPartitionStep_buildsStepWithExpectedName() {
        Step result = sut.catalogPartitionStep(jobRepository, partitioner, step);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("catalogPartitionStep");
    }

    @Test
    void catalogWorkerStep_buildsStepWithExpectedName() {
        Step result = sut.catalogWorkerStep(jobRepository, transactionManager, catalogFileItemReader,
                catalogAssetItemProcessor, catalogAssetItemWriter);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("catalogWorkerStep");
    }

    @Test
    void catalogFileItemReaderBean_constructsReaderForGivenFolder() {
        when(folderRepository.findByPath("/photos")).thenReturn(Optional.empty());
        when(storagePort.listFiles("/photos")).thenReturn(List.of());

        CatalogFileItemReader result = sut.catalogFileItemReader("/photos", assetRepository, folderRepository, storagePort);

        assertThat(result).isNotNull();
        assertThat(result.read()).isNull();
    }

    @Test
    void catalogAssetItemProcessorBean_constructsProcessor() {
        CatalogAssetItemProcessor result = sut.catalogAssetItemProcessor(storagePort, audioMetadataService);

        assertThat(result).isNotNull();
    }

    @Test
    void catalogAssetItemWriterBean_constructsWriterWithGivenParams() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        CatalogAssetItemWriter result = sut.catalogAssetItemWriter(1L, null, "/photos", assetRepository,
                assetExifRepository, assetAudioRepository, folderRepository, storagePort, thumbnailPort,
                kafkaTemplate, meterRegistry);

        assertThat(result).isNotNull();
    }

    @Test
    void asyncCatalogJobLauncher_constructsTaskExecutorJobLauncher() throws Exception {
        JobLauncher result = sut.asyncCatalogJobLauncher(jobRepository);

        assertThat(result).isInstanceOf(TaskExecutorJobLauncher.class);
    }
}
