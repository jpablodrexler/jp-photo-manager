package com.jpablodrexler.photomanager;

import com.jpablodrexler.photomanager.application.dto.AssetCatalogedEvent;
import com.jpablodrexler.photomanager.application.dto.AssetDeletedEvent;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.AddTagToAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.ListTagsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.RemoveTagFromAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end verification of the {@code redis-search-tag-cache} change: the {@code assets} cache
 * in front of {@code GetAssetsUseCaseImpl} and the {@code tags} cache in front of
 * {@code ListTagsUseCaseImpl}, both backed by a real Redis instance (reachable per the standard
 * local dev setup, same as every other {@code *IntegrationTest} in this suite that exercises the
 * shared {@code RedisConnectionFactory} bean — see {@code HomeStatsIntegrationTest} /
 * {@code UploadKafkaPipelineIntegrationTest}). Repeated identical calls are proven to be served
 * from the cache by mutating the underlying data directly through the repository (bypassing the
 * cache-populating use case) and observing that the stale result is initially still returned, then
 * that the relevant cache entries are correctly invalidated: {@code assets} via a real
 * {@code asset.cataloged}/{@code asset.deleted} Kafka event consumed by
 * {@link com.jpablodrexler.photomanager.infrastructure.kafka.AssetSearchCacheInvalidationListener},
 * and {@code tags} via the declarative {@code @CacheEvict} on the tag mutation use cases.
 */
@EmbeddedKafka(partitions = 1, topics = {
    "job.catalog.progress", "job.sync.progress", "job.convert.progress",
    "asset.cataloged", "asset.deleted", "asset.uploaded", "job.upload.progress"
})
class AssetSearchTagCacheIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    GetAssetsUseCase getAssetsUseCase;

    @Autowired
    ListTagsUseCase listTagsUseCase;

    @Autowired
    AddTagToAssetUseCase addTagToAssetUseCase;

    @Autowired
    RemoveTagFromAssetUseCase removeTagFromAssetUseCase;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    FolderRepository folderRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // The Spring test context (and therefore the real Redis-backed CacheManager) is reused
        // across test methods in this class, so a value cached by an earlier test would otherwise
        // leak into later tests since these tests mutate data directly through repositories rather
        // than exclusively through cache-evicting use cases (mirrors HomeStatsIntegrationTest's
        // same per-test cache-clear rationale for "home-stats").
        cacheManager.getCache("assets").clear();
        cacheManager.getCache("tags").clear();
    }

    @Test
    void getAssets_repeatedCallForSameFilter_secondCallServedFromCache() {
        Folder folder = saveFolder("assets-cache-hit");
        saveAsset(folder, "a.jpg", "hash-a-" + System.nanoTime());
        AssetFilter filter = filterFor(folder.getFolderId());

        PaginatedResult<Asset> first = getAssetsUseCase.execute(filter);
        assertThat(first.items()).hasSize(1);

        // Bypasses the cache-populating use case entirely, so the only way the second call could
        // reflect this new asset is if the first call's cache entry were NOT actually served.
        saveAsset(folder, "b.jpg", "hash-b-" + System.nanoTime());

        PaginatedResult<Asset> second = getAssetsUseCase.execute(filter);
        assertThat(second.items()).hasSize(1);
    }

    @Test
    void getAssets_afterAssetCatalogedEventForFolder_cacheInvalidatedAndNewAssetVisible() throws Exception {
        Folder folder = saveFolder("assets-cache-cataloged");
        saveAsset(folder, "a.jpg", "hash-a-" + System.nanoTime());
        AssetFilter filter = filterFor(folder.getFolderId());

        getAssetsUseCase.execute(filter);
        Asset secondAsset = saveAsset(folder, "b.jpg", "hash-b-" + System.nanoTime());

        kafkaTemplate.send("asset.cataloged", String.valueOf(secondAsset.getAssetId()),
                new AssetCatalogedEvent(secondAsset.getAssetId(), folder.getPath(), Instant.now()));

        pollUntil(() -> getAssetsUseCase.execute(filter).items().size() == 2, "assets cache to be invalidated after asset.cataloged");
    }

    @Test
    void getAssets_afterAssetDeletedEventForFolder_cacheInvalidatedAndDeletionVisible() throws Exception {
        Folder folder = saveFolder("assets-cache-deleted");
        saveAsset(folder, "a.jpg", "hash-a-" + System.nanoTime());
        Asset toDelete = saveAsset(folder, "b.jpg", "hash-b-" + System.nanoTime());
        AssetFilter filter = filterFor(folder.getFolderId());

        PaginatedResult<Asset> primed = getAssetsUseCase.execute(filter);
        assertThat(primed.items()).hasSize(2);

        assetRepository.deleteById(toDelete.getAssetId());
        kafkaTemplate.send("asset.deleted", String.valueOf(toDelete.getAssetId()),
                new AssetDeletedEvent(toDelete.getAssetId(), folder.getFolderId(), folder.getPath(), Instant.now(), true));

        pollUntil(() -> getAssetsUseCase.execute(filter).items().size() == 1, "assets cache to be invalidated after asset.deleted");
    }

    @Test
    void getAssets_differentFolder_notAffectedByOtherFoldersEviction() throws Exception {
        Folder folderA = saveFolder("assets-cache-folder-a");
        Folder folderB = saveFolder("assets-cache-folder-b");
        saveAsset(folderA, "a.jpg", "hash-a-" + System.nanoTime());
        saveAsset(folderB, "b.jpg", "hash-b-" + System.nanoTime());
        AssetFilter filterA = filterFor(folderA.getFolderId());
        AssetFilter filterB = filterFor(folderB.getFolderId());

        getAssetsUseCase.execute(filterA);
        getAssetsUseCase.execute(filterB);

        Asset newAssetInA = saveAsset(folderA, "a2.jpg", "hash-a2-" + System.nanoTime());
        kafkaTemplate.send("asset.cataloged", String.valueOf(newAssetInA.getAssetId()),
                new AssetCatalogedEvent(newAssetInA.getAssetId(), folderA.getPath(), Instant.now()));

        pollUntil(() -> getAssetsUseCase.execute(filterA).items().size() == 2, "folder A's cache to be invalidated");
        // Folder B's cache entry must remain untouched by folder A's eviction.
        assertThat(getAssetsUseCase.execute(filterB).items()).hasSize(1);
    }

    @Test
    void listTags_repeatedUnfilteredCall_secondCallServedFromCache() {
        String tagName = "cache-hit-" + System.nanoTime();
        addTagToAssetUseCase.execute(saveTaggableAsset(), tagName, null);

        List<Tag> first = listTagsUseCase.execute(null);
        assertThat(first).extracting(Tag::getName).contains(tagName);

        // Bypasses tagRepository through AddTagToAssetUseCaseImpl entirely, so its @CacheEvict never
        // runs — the only way the second call could reflect this new tag is a stale cache miss.
        String bypassTagName = "bypass-" + System.nanoTime();
        tagRepository.save(Tag.builder().name(bypassTagName).build());

        List<Tag> second = listTagsUseCase.execute(null);
        assertThat(second).extracting(Tag::getName).doesNotContain(bypassTagName);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void listTags_afterAddTagToAsset_tagsCacheEvictedAndNewTagVisible() {
        Folder folder = saveFolder("tags-cache-add");
        Asset asset = saveAsset(folder, "tagged.jpg", "hash-tag-" + System.nanoTime());

        listTagsUseCase.execute(null);

        String newTagName = "new-tag-" + System.nanoTime();
        addTagToAssetUseCase.execute(asset.getAssetId(), newTagName, null);

        List<Tag> afterAdd = listTagsUseCase.execute(null);
        assertThat(afterAdd).extracting(Tag::getName).contains(newTagName);
    }

    @Test
    void listTags_afterRemoveTagFromAsset_tagsCacheEvictedAndRemovalVisible() {
        Folder folder = saveFolder("tags-cache-remove");
        Asset asset = saveAsset(folder, "tagged.jpg", "hash-tag-" + System.nanoTime());
        String tagName = "removable-" + System.nanoTime();
        addTagToAssetUseCase.execute(asset.getAssetId(), tagName, null);

        listTagsUseCase.execute(null);

        removeTagFromAssetUseCase.execute(asset.getAssetId(), tagName, null);

        List<Tag> afterRemove = listTagsUseCase.execute(null);
        assertThat(afterRemove).extracting(Tag::getName).doesNotContain(tagName);
    }

    private Long saveTaggableAsset() {
        Folder folder = saveFolder("tags-cache");
        return saveAsset(folder, "tagged.jpg", "hash-tag-" + System.nanoTime()).getAssetId();
    }

    private Folder saveFolder(String nameHint) {
        Folder folder = new Folder();
        folder.setPath("/test/" + nameHint + "-" + System.nanoTime());
        return folderRepository.save(folder);
    }

    private Asset saveAsset(Folder folder, String fileName, String hash) {
        Asset asset = new Asset();
        asset.setFolder(folder);
        asset.setFileName(fileName);
        asset.setFileSize(100L);
        asset.setHash(hash);
        asset.setFileCreationDateTime(LocalDateTime.now());
        return assetRepository.save(asset);
    }

    private AssetFilter filterFor(Long folderId) {
        return new AssetFilter(folderId, null, null, null, null, null, 0, 20, false, null);
    }

    private void pollUntil(java.util.function.BooleanSupplier condition, String description) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Timed out waiting for " + description);
    }
}
