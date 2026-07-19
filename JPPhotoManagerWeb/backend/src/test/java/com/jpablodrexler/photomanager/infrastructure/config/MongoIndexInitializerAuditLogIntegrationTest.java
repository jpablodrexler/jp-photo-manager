package com.jpablodrexler.photomanager.infrastructure.config;

import com.jpablodrexler.photomanager.infrastructure.persistence.document.AuditLogDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that {@link MongoIndexInitializer} creates the compound {@code userId}/{@code timestamp}
 * index and the TTL index on {@code timestamp} for the {@code asset_audit_log} collection at
 * application startup, and that re-running index creation against an existing collection is
 * idempotent (mirrors the {@code mongodb-exif-store} integration test style).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@EnabledIfDockerAvailable
class MongoIndexInitializerAuditLogIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8");

    @Autowired
    MongoTemplate mongoTemplate;

    @Test
    void applicationStartup_auditLogCollection_hasUserIdTimestampCompoundIndex() {
        List<IndexInfo> indexes = mongoTemplate.indexOps(AuditLogDocument.class).getIndexInfo();

        boolean hasCompoundIndex = indexes.stream().anyMatch(index -> {
            List<String> keys = index.getIndexFields().stream().map(IndexField::getKey).toList();
            return keys.equals(List.of("userId", "timestamp"));
        });

        assertThat(hasCompoundIndex).isTrue();
    }

    @Test
    void applicationStartup_auditLogCollection_hasTimestampTtlIndex() {
        List<IndexInfo> indexes = mongoTemplate.indexOps(AuditLogDocument.class).getIndexInfo();

        boolean hasTtlIndex = indexes.stream().anyMatch(index ->
                index.getIndexFields().size() == 1
                        && "timestamp".equals(index.getIndexFields().getFirst().getKey())
                        && index.getExpireAfter().isPresent()
                        && index.getExpireAfter().get().equals(Duration.ofDays(365)));

        assertThat(hasTtlIndex).isTrue();
    }

    @Test
    void reRunningIndexCreation_existingCollection_isIdempotent() {
        assertThatCode(() -> {
            mongoTemplate.indexOps(AuditLogDocument.class)
                    .ensureIndex(new Index().on("userId", Sort.Direction.ASC).on("timestamp", Sort.Direction.DESC));
            mongoTemplate.indexOps(AuditLogDocument.class)
                    .ensureIndex(new Index().on("timestamp", Sort.Direction.ASC).expire(Duration.ofDays(365)));
        }).doesNotThrowAnyException();

        List<IndexInfo> indexes = mongoTemplate.indexOps(AuditLogDocument.class).getIndexInfo();
        long compoundIndexCount = indexes.stream()
                .filter(index -> index.getIndexFields().size() == 2)
                .count();
        assertThat(compoundIndexCount).isEqualTo(1);
    }
}
