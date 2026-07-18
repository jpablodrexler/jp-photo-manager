package com.jpablodrexler.photomanager.application.usecase.tag;

import com.jpablodrexler.photomanager.application.exception.AssetNotFoundException;
import com.jpablodrexler.photomanager.application.exception.TagNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.AssetSearchCachePort;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class TagUseCasesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class AddTagToAssetUseCaseImplTest {

        @Mock
        AssetRepository assetRepository;
        @Mock
        TagRepository tagRepository;
        @Mock
        AuditLogRepository auditLogRepository;
        @Mock
        AssetSearchCachePort assetSearchCachePort;
        @InjectMocks
        AddTagToAssetUseCaseImpl sut;

        @Test
        void execute_newTag_createsTagAndAddsToAsset() {
            when(assetRepository.existsById(1L)).thenReturn(true);
            when(tagRepository.findByName("vacation")).thenReturn(Optional.empty());
            Tag savedTag = Tag.builder().tagId(10L).name("vacation").build();
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            sut.execute(1L, "vacation", null);

            verify(tagRepository).save(argThat(t -> "vacation".equals(t.getName())));
            verify(assetRepository).addTagToAsset(1L, 10L);
        }

        @Test
        void execute_taggedAsset_evictsAssetSearchCacheForItsFolder() {
            when(assetRepository.existsById(1L)).thenReturn(true);
            Tag existing = Tag.builder().tagId(5L).name("vacation").build();
            when(tagRepository.findByName("vacation")).thenReturn(Optional.of(existing));
            Folder folder = Folder.builder().folderId(7L).path("/photos").build();
            Asset asset = Asset.builder().assetId(1L).folder(folder).build();
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

            sut.execute(1L, "vacation", null);

            verify(assetSearchCachePort).evictFolder(7L);
        }

        @Test
        void execute_existingTag_reusesTagAndAddsToAsset() {
            Tag existing = Tag.builder().tagId(5L).name("family").build();
            when(assetRepository.existsById(2L)).thenReturn(true);
            when(tagRepository.findByName("family")).thenReturn(Optional.of(existing));

            sut.execute(2L, "family", null);

            verify(tagRepository, never()).save(any());
            verify(assetRepository).addTagToAsset(2L, 5L);
        }

        @Test
        void execute_uppercaseName_normalizesToLowercase() {
            when(assetRepository.existsById(1L)).thenReturn(true);
            when(tagRepository.findByName("vacation")).thenReturn(Optional.empty());
            Tag savedTag = Tag.builder().tagId(10L).name("vacation").build();
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

            sut.execute(1L, "VACATION", null);

            verify(tagRepository).findByName("vacation");
        }

        @Test
        void execute_assetNotFound_throwsAssetNotFoundException() {
            when(assetRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> sut.execute(99L, "vacation", null))
                    .isInstanceOf(AssetNotFoundException.class);

            verify(assetRepository, never()).addTagToAsset(anyLong(), anyLong());
        }

        @Test
        void execute_duplicateAssignment_idempotent() {
            when(assetRepository.existsById(1L)).thenReturn(true);
            Tag existing = Tag.builder().tagId(5L).name("vacation").build();
            when(tagRepository.findByName("vacation")).thenReturn(Optional.of(existing));

            sut.execute(1L, "vacation", null);

            verify(assetRepository).addTagToAsset(1L, 5L);
        }

        @Test
        void execute_tagAdded_logsAssetTaggedAuditEvent() {
            when(assetRepository.existsById(1L)).thenReturn(true);
            Tag existing = Tag.builder().tagId(5L).name("vacation").build();
            when(tagRepository.findByName("vacation")).thenReturn(Optional.of(existing));

            sut.execute(1L, "vacation", null);

            verify(auditLogRepository).log(any());
        }

        @Test
        void execute_auditLogThrows_doesNotPropagate() {
            when(assetRepository.existsById(1L)).thenReturn(true);
            Tag existing = Tag.builder().tagId(5L).name("vacation").build();
            when(tagRepository.findByName("vacation")).thenReturn(Optional.of(existing));
            doThrow(new RuntimeException("mongo down")).when(auditLogRepository).log(any());

            assertThatCode(() -> sut.execute(1L, "vacation", null)).doesNotThrowAnyException();

            verify(assetRepository).addTagToAsset(1L, 5L);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class RemoveTagFromAssetUseCaseImplTest {

        @Mock
        AssetRepository assetRepository;
        @Mock
        TagRepository tagRepository;
        @Mock
        AuditLogRepository auditLogRepository;
        @Mock
        AssetSearchCachePort assetSearchCachePort;
        @InjectMocks
        RemoveTagFromAssetUseCaseImpl sut;

        @Test
        void execute_tagAssignedToAsset_removesAssignment() {
            Tag tag = Tag.builder().tagId(10L).name("vacation").build();
            when(tagRepository.findByName("vacation")).thenReturn(Optional.of(tag));
            when(assetRepository.removeTagFromAsset(1L, 10L)).thenReturn(1);
            when(tagRepository.isUsedByOtherAssets(10L, 1L)).thenReturn(true);

            sut.execute(1L, "vacation", null);

            verify(assetRepository).removeTagFromAsset(1L, 10L);
            verify(tagRepository, never()).deleteById(anyLong());
        }

        @Test
        void execute_tagRemoved_evictsAssetSearchCacheForItsFolder() {
            Tag tag = Tag.builder().tagId(10L).name("vacation").build();
            when(tagRepository.findByName("vacation")).thenReturn(Optional.of(tag));
            when(assetRepository.removeTagFromAsset(1L, 10L)).thenReturn(1);
            when(tagRepository.isUsedByOtherAssets(10L, 1L)).thenReturn(true);
            Folder folder = Folder.builder().folderId(9L).path("/photos").build();
            Asset asset = Asset.builder().assetId(1L).folder(folder).build();
            when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

            sut.execute(1L, "vacation", null);

            verify(assetSearchCachePort).evictFolder(9L);
        }

        @Test
        void execute_lastAssetWithTag_deletesOrphanTag() {
            Tag tag = Tag.builder().tagId(10L).name("rare-tag").build();
            when(tagRepository.findByName("rare-tag")).thenReturn(Optional.of(tag));
            when(assetRepository.removeTagFromAsset(1L, 10L)).thenReturn(1);
            when(tagRepository.isUsedByOtherAssets(10L, 1L)).thenReturn(false);

            sut.execute(1L, "rare-tag", null);

            verify(tagRepository).deleteById(10L);
        }

        @Test
        void execute_tagNotAssignedToAsset_throwsNoSuchElementException() {
            Tag tag = Tag.builder().tagId(10L).name("vacation").build();
            when(tagRepository.findByName("vacation")).thenReturn(Optional.of(tag));
            when(assetRepository.removeTagFromAsset(1L, 10L)).thenReturn(0);

            assertThatThrownBy(() -> sut.execute(1L, "vacation", null))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void execute_tagNotFound_throwsTagNotFoundException() {
            when(tagRepository.findByName("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(1L, "nonexistent", null))
                    .isInstanceOf(TagNotFoundException.class);

            verify(assetRepository, never()).removeTagFromAsset(anyLong(), anyLong());
        }

        @Test
        void execute_uppercaseName_normalizesToLowercase() {
            when(tagRepository.findByName("vacation")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(1L, "VACATION", null))
                    .isInstanceOf(TagNotFoundException.class);

            verify(tagRepository).findByName("vacation");
        }

        @Test
        void execute_tagRemoved_logsAssetUntaggedAuditEvent() {
            Tag tag = Tag.builder().tagId(10L).name("vacation").build();
            when(tagRepository.findByName("vacation")).thenReturn(Optional.of(tag));
            when(assetRepository.removeTagFromAsset(1L, 10L)).thenReturn(1);
            when(tagRepository.isUsedByOtherAssets(10L, 1L)).thenReturn(true);

            sut.execute(1L, "vacation", null);

            verify(auditLogRepository).log(any());
        }

        @Test
        void execute_auditLogThrows_doesNotPropagate() {
            Tag tag = Tag.builder().tagId(10L).name("vacation").build();
            when(tagRepository.findByName("vacation")).thenReturn(Optional.of(tag));
            when(assetRepository.removeTagFromAsset(1L, 10L)).thenReturn(1);
            when(tagRepository.isUsedByOtherAssets(10L, 1L)).thenReturn(true);
            doThrow(new RuntimeException("mongo down")).when(auditLogRepository).log(any());

            assertThatCode(() -> sut.execute(1L, "vacation", null)).doesNotThrowAnyException();
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class BulkAddTagUseCaseImplTest {

        @Mock
        AssetRepository assetRepository;
        @Mock
        TagRepository tagRepository;
        @Mock
        AuditLogRepository auditLogRepository;
        @Mock
        AssetSearchCachePort assetSearchCachePort;
        @InjectMocks
        BulkAddTagUseCaseImpl sut;

        @Test
        void execute_newTag_createsTagAndAddsToAllAssets() {
            when(tagRepository.findByName("to-print")).thenReturn(Optional.empty());
            Tag saved = Tag.builder().tagId(20L).name("to-print").build();
            when(tagRepository.save(any())).thenReturn(saved);

            sut.execute(List.of(1L, 2L, 3L), "to-print", null);

            verify(assetRepository).addTagToAsset(1L, 20L);
            verify(assetRepository).addTagToAsset(2L, 20L);
            verify(assetRepository).addTagToAsset(3L, 20L);
        }

        @Test
        void execute_assetsInDifferentFolders_evictsEachDistinctFolderOnce() {
            when(tagRepository.findByName("to-print")).thenReturn(Optional.empty());
            Tag saved = Tag.builder().tagId(20L).name("to-print").build();
            when(tagRepository.save(any())).thenReturn(saved);
            Folder folderA = Folder.builder().folderId(1L).path("/a").build();
            Folder folderB = Folder.builder().folderId(2L).path("/b").build();
            when(assetRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(
                    Asset.builder().assetId(1L).folder(folderA).build(),
                    Asset.builder().assetId(2L).folder(folderA).build(),
                    Asset.builder().assetId(3L).folder(folderB).build()));

            sut.execute(List.of(1L, 2L, 3L), "to-print", null);

            verify(assetSearchCachePort).evictFolder(1L);
            verify(assetSearchCachePort).evictFolder(2L);
        }

        @Test
        void execute_existingTag_reusesTagForAllAssets() {
            Tag existing = Tag.builder().tagId(15L).name("family").build();
            when(tagRepository.findByName("family")).thenReturn(Optional.of(existing));

            sut.execute(List.of(5L, 6L), "family", null);

            verify(tagRepository, never()).save(any());
            verify(assetRepository).addTagToAsset(5L, 15L);
            verify(assetRepository).addTagToAsset(6L, 15L);
        }

        @Test
        void execute_taggedAssets_writesAuditEventPerAsset() {
            Tag existing = Tag.builder().tagId(15L).name("family").build();
            when(tagRepository.findByName("family")).thenReturn(Optional.of(existing));

            sut.execute(List.of(5L, 6L), "family", null);

            verify(auditLogRepository, times(2)).log(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class BulkRemoveTagUseCaseImplTest {

        @Mock
        AssetRepository assetRepository;
        @Mock
        TagRepository tagRepository;
        @Mock
        AuditLogRepository auditLogRepository;
        @Mock
        AssetSearchCachePort assetSearchCachePort;
        @InjectMocks
        BulkRemoveTagUseCaseImpl sut;

        @Test
        void execute_tagExists_removesFromAllAssets() {
            Tag tag = Tag.builder().tagId(10L).name("draft").build();
            when(tagRepository.findByName("draft")).thenReturn(Optional.of(tag));
            when(tagRepository.isUsedByOtherAssets(10L, -1L)).thenReturn(true);

            sut.execute(List.of(1L, 2L, 3L), "draft", null);

            verify(assetRepository).removeTagFromAsset(1L, 10L);
            verify(assetRepository).removeTagFromAsset(2L, 10L);
            verify(assetRepository).removeTagFromAsset(3L, 10L);
        }

        @Test
        void execute_assetsInDifferentFolders_evictsEachDistinctFolderOnce() {
            Tag tag = Tag.builder().tagId(10L).name("draft").build();
            when(tagRepository.findByName("draft")).thenReturn(Optional.of(tag));
            when(tagRepository.isUsedByOtherAssets(10L, -1L)).thenReturn(true);
            Folder folderA = Folder.builder().folderId(1L).path("/a").build();
            Folder folderB = Folder.builder().folderId(2L).path("/b").build();
            when(assetRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                    Asset.builder().assetId(1L).folder(folderA).build(),
                    Asset.builder().assetId(2L).folder(folderB).build()));

            sut.execute(List.of(1L, 2L), "draft", null);

            verify(assetSearchCachePort).evictFolder(1L);
            verify(assetSearchCachePort).evictFolder(2L);
        }

        @Test
        void execute_tagNotFound_silentlyReturns() {
            when(tagRepository.findByName("nonexistent")).thenReturn(Optional.empty());

            sut.execute(List.of(1L, 2L), "nonexistent", null);

            verify(assetRepository, never()).removeTagFromAsset(anyLong(), anyLong());
        }

        @Test
        void execute_tagNoLongerUsed_deletesOrphanTag() {
            Tag tag = Tag.builder().tagId(10L).name("rare").build();
            when(tagRepository.findByName("rare")).thenReturn(Optional.of(tag));
            when(tagRepository.isUsedByOtherAssets(10L, -1L)).thenReturn(false);

            sut.execute(List.of(1L), "rare", null);

            verify(tagRepository).deleteById(10L);
        }

        @Test
        void execute_untaggedAssets_writesAuditEventPerAsset() {
            Tag tag = Tag.builder().tagId(10L).name("draft").build();
            when(tagRepository.findByName("draft")).thenReturn(Optional.of(tag));
            when(tagRepository.isUsedByOtherAssets(10L, -1L)).thenReturn(true);

            sut.execute(List.of(1L, 2L, 3L), "draft", null);

            verify(auditLogRepository, times(3)).log(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ListTagsUseCaseImplTest {

        @Mock
        TagRepository tagRepository;
        @InjectMocks
        ListTagsUseCaseImpl sut;

        @Test
        void execute_withQuery_delegatesToRepositoryWithLimit20() {
            List<Tag> tags = List.of(Tag.builder().name("vacation").build());
            when(tagRepository.findByNameContaining("vac", 20)).thenReturn(tags);

            List<Tag> result = sut.execute("vac");

            assertThat(result).containsExactlyElementsOf(tags);
            verify(tagRepository).findByNameContaining("vac", 20);
        }

        @Test
        void execute_withNullQuery_delegatesWithEmptyString() {
            when(tagRepository.findByNameContaining("", 20)).thenReturn(List.of());

            sut.execute(null);

            verify(tagRepository).findByNameContaining("", 20);
        }

        @Test
        void execute_withBlankQuery_trimsToEmpty() {
            when(tagRepository.findByNameContaining("", 20)).thenReturn(List.of());

            sut.execute("   ");

            verify(tagRepository).findByNameContaining("", 20);
        }
    }
}
