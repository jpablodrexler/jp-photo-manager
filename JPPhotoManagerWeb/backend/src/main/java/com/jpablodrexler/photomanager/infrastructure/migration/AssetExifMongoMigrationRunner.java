package com.jpablodrexler.photomanager.infrastructure.migration;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.port.out.AssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetExifEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaAssetExifRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AssetExifEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * One-time migration of the PostgreSQL {@code asset_exif} table into MongoDB. Runs on every
 * startup but exits immediately once a completion marker document exists in the
 * {@code migration_status} collection. Idempotent: re-processing a batch upserts documents keyed
 * by {@code assetId} (via {@link AssetExifRepository#save(AssetExif)}), so a resumed run after a
 * crash simply re-copies already-migrated rows as a harmless no-op.
 *
 * <p>Removed once the corresponding Flyway migration drops the PostgreSQL {@code asset_exif}
 * table (see the change's "Deploy 2" step) — kept until then as the safety net described in the
 * {@code mongodb-exif-store} design.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssetExifMongoMigrationRunner implements CommandLineRunner {

    static final String MARKER_COLLECTION = "migration_status";
    static final String MARKER_ID = "asset_exif_pg_migration";
    private static final int BATCH_SIZE = 500;

    private final JpaAssetExifRepository jpaAssetExifRepository;
    private final AssetExifEntityMapper entityMapper;
    private final AssetExifRepository assetExifRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        if (isAlreadyMigrated()) {
            log.info("asset_exif PostgreSQL -> MongoDB migration already completed; skipping");
            return;
        }

        long total = jpaAssetExifRepository.count();
        long processed = 0;
        int pageNumber = 0;
        Page<AssetExifEntity> page;
        do {
            page = jpaAssetExifRepository.findAll(PageRequest.of(pageNumber, BATCH_SIZE));
            for (AssetExifEntity entity : page.getContent()) {
                AssetExif domain = entityMapper.toDomain(entity);
                assetExifRepository.save(domain);
            }
            processed += page.getNumberOfElements();
            log.info("asset_exif migration progress: {}/{} rows processed", processed, total);
            pageNumber++;
        } while (page.hasNext());

        markMigrationComplete();
        log.info("asset_exif PostgreSQL -> MongoDB migration completed: {} rows migrated", processed);
    }

    private boolean isAlreadyMigrated() {
        Query query = Query.query(Criteria.where("_id").is(MARKER_ID));
        return mongoTemplate.exists(query, MARKER_COLLECTION);
    }

    private void markMigrationComplete() {
        Document marker = new Document("_id", MARKER_ID).append("completedAt", Instant.now());
        mongoTemplate.save(marker, MARKER_COLLECTION);
    }
}
