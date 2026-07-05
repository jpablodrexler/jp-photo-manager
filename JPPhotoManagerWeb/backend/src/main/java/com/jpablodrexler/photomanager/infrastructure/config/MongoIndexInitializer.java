package com.jpablodrexler.photomanager.infrastructure.config;

import com.jpablodrexler.photomanager.infrastructure.persistence.document.AssetExifDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Ensures the MongoDB indexes required by the {@code asset_exif} collection exist at application
 * startup: a unique index on {@code assetId} (the business key used for lookups) and a
 * {@code 2dsphere} index on {@code location} (for future proximity queries). {@code ensureIndex}
 * is idempotent — a no-op when the index already exists.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexInitializer implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps(AssetExifDocument.class)
                .ensureIndex(new Index().on("assetId", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps(AssetExifDocument.class)
                .ensureIndex(new GeospatialIndex("location").typed(GeoSpatialIndexType.GEO_2DSPHERE));
        log.info("Ensured MongoDB indexes on asset_exif collection (assetId unique, location 2dsphere)");
    }
}
