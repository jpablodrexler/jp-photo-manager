package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.SyncProgressMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Proves the per-instance consumer-group fix (kafka-sse-broadcast, #80): two
// independently-configured listener containers, each with its own unique
// group id (simulating two application instances), both receive the same
// single-partition message instead of Kafka splitting delivery between them.
@EmbeddedKafka(partitions = 1, topics = {"job.sync.progress"})
class KafkaProgressListenerMultiInstanceTest {

    private static final String TOPIC = "job.sync.progress";

    @Test
    void twoContainers_distinctGroupIds_bothReceiveSameMessage(EmbeddedKafkaBroker embeddedKafka) throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        CopyOnWriteArrayList<SyncProgressMessage> instanceAReceived = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<SyncProgressMessage> instanceBReceived = new CopyOnWriteArrayList<>();

        KafkaMessageListenerContainer<String, SyncProgressMessage> containerA =
                startListenerContainer(embeddedKafka, "sse-broadcaster-" + UUID.randomUUID(),
                        record -> {
                            instanceAReceived.add(record.value());
                            latch.countDown();
                        });
        KafkaMessageListenerContainer<String, SyncProgressMessage> containerB =
                startListenerContainer(embeddedKafka, "sse-broadcaster-" + UUID.randomUUID(),
                        record -> {
                            instanceBReceived.add(record.value());
                            latch.countDown();
                        });

        try {
            ContainerTestUtils.waitForAssignment(containerA, embeddedKafka.getPartitionsPerTopic());
            ContainerTestUtils.waitForAssignment(containerB, embeddedKafka.getPartitionsPerTopic());

            KafkaTemplate<String, SyncProgressMessage> template = buildProducer(embeddedKafka);
            SyncProgressMessage message = SyncProgressMessage.progress(7L, "Syncing files...");
            template.send(new ProducerRecord<>(TOPIC, message)).get(10, TimeUnit.SECONDS);

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(instanceAReceived).containsExactly(message);
            assertThat(instanceBReceived).containsExactly(message);
        } finally {
            containerA.stop();
            containerB.stop();
        }
    }

    private KafkaMessageListenerContainer<String, SyncProgressMessage> startListenerContainer(
            EmbeddedKafkaBroker embeddedKafka, String groupId, MessageListener<String, SyncProgressMessage> listener) {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "false", embeddedKafka);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.jpablodrexler.photomanager.application.dto");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SyncProgressMessage.class.getName());

        DefaultKafkaConsumerFactory<String, SyncProgressMessage> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties(TOPIC);
        containerProperties.setMessageListener(listener);

        KafkaMessageListenerContainer<String, SyncProgressMessage> container =
                new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setBeanName(groupId);
        container.start();
        return container;
    }

    private KafkaTemplate<String, SyncProgressMessage> buildProducer(EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        DefaultKafkaProducerFactory<String, SyncProgressMessage> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        return new KafkaTemplate<>(producerFactory);
    }
}
