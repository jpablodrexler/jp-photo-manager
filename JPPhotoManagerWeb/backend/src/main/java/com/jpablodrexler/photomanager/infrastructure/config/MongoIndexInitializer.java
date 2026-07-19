package com.jpablodrexler.photomanager.infrastructure.config;

import com.jpablodrexler.photomanager.infrastructure.persistence.document.AuditLogDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Ensures the MongoDB indexes required by the {@code asset_audit_log} collection exist at
 * application startup: a compound {@code userId}/{@code timestamp} index and a TTL index on
 * {@code timestamp} (365-day expiry). {@code ensureIndex} is idempotent — a no-op when the index
 * already exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexInitializer implements ApplicationRunner {

    private static final int AUDIT_LOG_RETENTION_DAYS = 365;

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps(AuditLogDocument.class)
                .ensureIndex(new Index().on("userId", Sort.Direction.ASC).on("timestamp", Sort.Direction.DESC));
        mongoTemplate.indexOps(AuditLogDocument.class)
                .ensureIndex(new Index().on("timestamp", Sort.Direction.ASC)
                        .expire(Duration.ofDays(AUDIT_LOG_RETENTION_DAYS)));
        log.info("Ensured MongoDB indexes on asset_audit_log collection (userId+timestamp compound, "
                + "timestamp TTL {} days)", AUDIT_LOG_RETENTION_DAYS);
    }
}
