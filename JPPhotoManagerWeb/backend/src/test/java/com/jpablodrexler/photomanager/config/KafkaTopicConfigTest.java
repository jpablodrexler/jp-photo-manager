package com.jpablodrexler.photomanager.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the declared topic names/partitions/replicas for each {@code NewTopic} bean this
 * configuration wires up, so a typo in a topic name (which would silently create a
 * differently-named topic rather than fail fast) is caught by a test rather than only in
 * production logs.
 */
class KafkaTopicConfigTest {

    KafkaTopicConfig sut = new KafkaTopicConfig();

    @Test
    void catalogProgressTopic_hasExpectedNameAndPartitions() {
        NewTopic topic = sut.catalogProgressTopic();

        assertThat(topic.name()).isEqualTo("job.catalog.progress");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    void syncProgressTopic_hasExpectedNameAndPartitions() {
        NewTopic topic = sut.syncProgressTopic();

        assertThat(topic.name()).isEqualTo("job.sync.progress");
        assertThat(topic.numPartitions()).isEqualTo(1);
    }

    @Test
    void convertProgressTopic_hasExpectedNameAndPartitions() {
        NewTopic topic = sut.convertProgressTopic();

        assertThat(topic.name()).isEqualTo("job.convert.progress");
        assertThat(topic.numPartitions()).isEqualTo(1);
    }

    @Test
    void assetCatalogedTopic_hasExpectedNameAndPartitions() {
        NewTopic topic = sut.assetCatalogedTopic();

        assertThat(topic.name()).isEqualTo("asset.cataloged");
        assertThat(topic.numPartitions()).isEqualTo(3);
    }

    @Test
    void assetDeletedTopic_hasExpectedNameAndPartitions() {
        NewTopic topic = sut.assetDeletedTopic();

        assertThat(topic.name()).isEqualTo("asset.deleted");
        assertThat(topic.numPartitions()).isEqualTo(3);
    }

    @Test
    void assetUploadedTopic_hasExpectedNameAndPartitions() {
        NewTopic topic = sut.assetUploadedTopic();

        assertThat(topic.name()).isEqualTo("asset.uploaded");
        assertThat(topic.numPartitions()).isEqualTo(3);
    }

    @Test
    void uploadProgressTopic_hasExpectedNameAndPartitions() {
        NewTopic topic = sut.uploadProgressTopic();

        assertThat(topic.name()).isEqualTo("job.upload.progress");
        assertThat(topic.numPartitions()).isEqualTo(1);
    }
}
