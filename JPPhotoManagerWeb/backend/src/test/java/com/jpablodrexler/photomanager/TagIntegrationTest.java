package com.jpablodrexler.photomanager;

import com.jpablodrexler.photomanager.application.dto.AssetFilter;
import com.jpablodrexler.photomanager.application.dto.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.asset.GetAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.AddTagToAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.BulkAddTagUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.ListTagsUseCase;
import com.jpablodrexler.photomanager.domain.port.in.tag.RemoveTagFromAssetUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfDockerAvailable
class TagIntegrationTest extends PostgresIntegrationTest {

    @Autowired AddTagToAssetUseCase addTagToAssetUseCase;
    @Autowired RemoveTagFromAssetUseCase removeTagFromAssetUseCase;
    @Autowired BulkAddTagUseCase bulkAddTagUseCase;
    @Autowired ListTagsUseCase listTagsUseCase;
    @Autowired GetAssetsUseCase getAssetsUseCase;
    @Autowired AssetRepository assetRepository;
    @Autowired FolderRepository folderRepository;
    @Autowired TagRepository tagRepository;

    private Asset asset1;
    private Asset asset2;
    private Long folderId;

    @BeforeEach
    void setUp() {
        assetRepository.findAll().forEach(a -> assetRepository.deleteById(a.getAssetId()));

        Folder folder = new Folder();
        folder.setPath("/test/tag-photos_" + System.nanoTime());
        folder = folderRepository.save(folder);
        folderId = folder.getFolderId();

        asset1 = new Asset();
        asset1.setFolder(folder);
        asset1.setFileName("a.jpg");
        asset1.setFileSize(1000L);
        asset1.setHash("hash-a-" + System.nanoTime());
        asset1.setThumbnailCreationDateTime(LocalDateTime.now());
        asset1 = assetRepository.save(asset1);

        asset2 = new Asset();
        asset2.setFolder(folder);
        asset2.setFileName("b.jpg");
        asset2.setFileSize(2000L);
        asset2.setHash("hash-b-" + System.nanoTime());
        asset2.setThumbnailCreationDateTime(LocalDateTime.now());
        asset2 = assetRepository.save(asset2);
    }

    @Test
    void addTag_filterByTag_removeTag_fullLifecycle() {
        addTagToAssetUseCase.execute(asset1.getAssetId(), "vacation");

        AssetFilter filterWithTag = new AssetFilter(folderId, null, null, null, null, null, 0, 50, false, Set.of("vacation"));
        PaginatedResult<Asset> filtered = getAssetsUseCase.execute(filterWithTag);
        assertThat(filtered.total()).isEqualTo(1);
        assertThat(filtered.items().getFirst().getAssetId()).isEqualTo(asset1.getAssetId());

        removeTagFromAssetUseCase.execute(asset1.getAssetId(), "vacation");

        AssetFilter filterAfterRemove = new AssetFilter(folderId, null, null, null, null, null, 0, 50, false, Set.of("vacation"));
        PaginatedResult<Asset> afterRemove = getAssetsUseCase.execute(filterAfterRemove);
        assertThat(afterRemove.total()).isZero();

        assertThat(tagRepository.findByName("vacation")).isEmpty();
    }

    @Test
    void addTag_uppercaseName_normalizesToLowercase() {
        addTagToAssetUseCase.execute(asset1.getAssetId(), "FAMILY");

        List<com.jpablodrexler.photomanager.domain.model.Tag> tags = listTagsUseCase.execute("family");
        assertThat(tags).anyMatch(t -> "family".equals(t.getName()));
    }

    @Test
    void addTag_duplicateAssignment_isIdempotent() {
        addTagToAssetUseCase.execute(asset1.getAssetId(), "vacation");
        addTagToAssetUseCase.execute(asset1.getAssetId(), "vacation");

        List<com.jpablodrexler.photomanager.domain.model.Tag> tags = listTagsUseCase.execute("vacation");
        assertThat(tags).hasSize(1);
    }

    @Test
    void removeTag_tagStillUsedByOtherAsset_doesNotDeleteTag() {
        addTagToAssetUseCase.execute(asset1.getAssetId(), "vacation");
        addTagToAssetUseCase.execute(asset2.getAssetId(), "vacation");

        removeTagFromAssetUseCase.execute(asset1.getAssetId(), "vacation");

        assertThat(tagRepository.findByName("vacation")).isPresent();
    }

    @Test
    void removeTag_notAssigned_throws() {
        assertThatThrownBy(() -> removeTagFromAssetUseCase.execute(asset1.getAssetId(), "nonexistent"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void filterByMultipleTags_andSemantics_returnsOnlyAssetsWithAllTags() {
        addTagToAssetUseCase.execute(asset1.getAssetId(), "vacation");
        addTagToAssetUseCase.execute(asset1.getAssetId(), "family");
        addTagToAssetUseCase.execute(asset2.getAssetId(), "vacation");

        AssetFilter filter = new AssetFilter(folderId, null, null, null, null, null, 0, 50, false, Set.of("vacation", "family"));
        PaginatedResult<Asset> result = getAssetsUseCase.execute(filter);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items().getFirst().getAssetId()).isEqualTo(asset1.getAssetId());
    }

    @Test
    void bulkAddTag_addsTagToAllAssets() {
        bulkAddTagUseCase.execute(List.of(asset1.getAssetId(), asset2.getAssetId()), "to-print");

        AssetFilter filter = new AssetFilter(folderId, null, null, null, null, null, 0, 50, false, Set.of("to-print"));
        PaginatedResult<Asset> result = getAssetsUseCase.execute(filter);

        assertThat(result.total()).isEqualTo(2);
    }

    @Test
    void listTags_withQuery_returnsMatchingTags() {
        addTagToAssetUseCase.execute(asset1.getAssetId(), "vacation");
        addTagToAssetUseCase.execute(asset2.getAssetId(), "vaccinated");

        List<com.jpablodrexler.photomanager.domain.model.Tag> tags = listTagsUseCase.execute("vac");

        assertThat(tags).extracting(com.jpablodrexler.photomanager.domain.model.Tag::getName)
                .containsExactlyInAnyOrder("vacation", "vaccinated");
    }
}
