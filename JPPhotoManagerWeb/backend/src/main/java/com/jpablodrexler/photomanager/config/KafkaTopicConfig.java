package com.jpablodrexler.photomanager.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic catalogProgressTopic() {
        return TopicBuilder.name("job.catalog.progress")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", String.valueOf(60 * 60 * 1000L))
                .build();
    }

    @Bean
    public NewTopic syncProgressTopic() {
        return TopicBuilder.name("job.sync.progress")
                .partitions(1)
                .replicas(1)
                .config("retention.ms", String.valueOf(60 * 60 * 1000L))
                .build();
    }

    @Bean
    public NewTopic convertProgressTopic() {
        return TopicBuilder.name("job.convert.progress")
                .partitions(1)
                .replicas(1)
                .config("retention.ms", String.valueOf(60 * 60 * 1000L))
                .build();
    }

    @Bean
    public NewTopic assetCatalogedTopic() {
        return TopicBuilder.name("asset.cataloged")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000))
                .build();
    }

    @Bean
    public NewTopic assetDeletedTopic() {
        return TopicBuilder.name("asset.deleted")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000))
                .build();
    }
}
