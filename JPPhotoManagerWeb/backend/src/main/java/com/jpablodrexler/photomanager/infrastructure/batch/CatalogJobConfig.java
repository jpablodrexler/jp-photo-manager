package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.port.out.AssetAudioRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import com.jpablodrexler.photomanager.infrastructure.service.AudioMetadataService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;

@Configuration
public class CatalogJobConfig {

    @Value("${photomanager.catalog-partition-grid-size:4}")
    private int gridSize;

    @Value("${photomanager.catalog-chunk-size:50}")
    private int chunkSize;

    @Bean
    public Job catalogJob(JobRepository jobRepository,
                           @Qualifier("catalogPartitionStep") Step catalogPartitionStep,
                           CatalogItemWriteListener catalogItemWriteListener) {
        return new JobBuilder("catalogJob", jobRepository)
                .start(catalogPartitionStep)
                .listener(catalogItemWriteListener)
                .build();
    }

    @Bean(name = "catalogPartitionStep")
    public Step catalogPartitionStep(JobRepository jobRepository,
                                      CatalogFolderPartitioner partitioner,
                                      @Qualifier("catalogWorkerStep") Step catalogWorkerStep) {
        ThreadPoolTaskExecutor partitionExecutor = new ThreadPoolTaskExecutor();
        partitionExecutor.setCorePoolSize(gridSize);
        partitionExecutor.setMaxPoolSize(gridSize);
        partitionExecutor.setThreadNamePrefix("catalog-partition-");
        partitionExecutor.initialize();

        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(partitionExecutor);
        handler.setStep(catalogWorkerStep);
        handler.setGridSize(gridSize);

        return new StepBuilder("catalogPartitionStep", jobRepository)
                .partitioner("catalogWorkerStep", partitioner)
                .partitionHandler(handler)
                .build();
    }

    @Bean(name = "catalogWorkerStep")
    public Step catalogWorkerStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   CatalogFileItemReader catalogFileItemReader,
                                   CatalogAssetItemProcessor catalogAssetItemProcessor,
                                   CatalogAssetItemWriter catalogAssetItemWriter) {
        return new StepBuilder("catalogWorkerStep", jobRepository)
                .<Path, CatalogBatchItem>chunk(chunkSize, transactionManager)
                .reader(catalogFileItemReader)
                .processor(catalogAssetItemProcessor)
                .writer(catalogAssetItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public CatalogFileItemReader catalogFileItemReader(
            @Value("#{stepExecutionContext['folderPath']}") String folderPath,
            AssetRepository assetRepository,
            FolderRepository folderRepository,
            StoragePort storagePort) {
        return new CatalogFileItemReader(folderPath, assetRepository, folderRepository, storagePort);
    }

    @Bean
    @StepScope
    public CatalogAssetItemProcessor catalogAssetItemProcessor(
            StoragePort storagePort,
            AudioMetadataService audioMetadataService) {
        return new CatalogAssetItemProcessor(storagePort, audioMetadataService);
    }

    @Bean
    @StepScope
    public CatalogAssetItemWriter catalogAssetItemWriter(
            @Value("#{jobParameters['runId']}") Long runId,
            @Value("#{stepExecutionContext['folderPath']}") String folderPath,
            AssetRepository assetRepository,
            AssetExifRepository assetExifRepository,
            AssetAudioRepository assetAudioRepository,
            FolderRepository folderRepository,
            StoragePort storagePort,
            ThumbnailPort thumbnailPort,
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry) {
        return new CatalogAssetItemWriter(runId, folderPath, assetRepository, assetExifRepository,
                assetAudioRepository, folderRepository, storagePort, thumbnailPort, kafkaTemplate, meterRegistry);
    }

    @Bean(name = "asyncCatalogJobLauncher")
    public JobLauncher asyncCatalogJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("catalog-batch-"));
        launcher.afterPropertiesSet();
        return launcher;
    }
}
