package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.enums.SortCriteria;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AssetSearchCacheKeyGeneratorTest {

    private final AssetSearchCacheKeyGenerator sut = new AssetSearchCacheKeyGenerator();

    @Test
    void generate_sameFilterTwice_producesSameKey() {
        AssetFilter filter = new AssetFilter(1L, "beach", LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31), 3, SortCriteria.FILE_CREATION_DATE_TIME, 0, 20, false, Set.of("vacation"));

        Object key1 = sut.generate(null, null, filter);
        Object key2 = sut.generate(null, null, filter);

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void generate_keyHasFolderIdPrefixForEviction() {
        AssetFilter filter = new AssetFilter(42L, null, null, null, null, null, 0, 20, false, null);

        Object key = sut.generate(null, null, filter);

        assertThat(key.toString()).startsWith("42:");
    }

    @Test
    void generate_differentPage_producesDifferentKey() {
        AssetFilter filterPage0 = new AssetFilter(1L, null, null, null, null, null, 0, 20, false, null);
        AssetFilter filterPage1 = new AssetFilter(1L, null, null, null, null, null, 1, 20, false, null);

        Object key0 = sut.generate(null, null, filterPage0);
        Object key1 = sut.generate(null, null, filterPage1);

        assertThat(key0).isNotEqualTo(key1);
    }

    @Test
    void generate_differentMinRating_producesDifferentKey() {
        AssetFilter filterRating3 = new AssetFilter(1L, null, null, null, 3, null, 0, 20, false, null);
        AssetFilter filterRating4 = new AssetFilter(1L, null, null, null, 4, null, 0, 20, false, null);

        Object key3 = sut.generate(null, null, filterRating3);
        Object key4 = sut.generate(null, null, filterRating4);

        assertThat(key3).isNotEqualTo(key4);
    }

    @Test
    void generate_differentTags_producesDifferentKey() {
        AssetFilter filterVacation = new AssetFilter(1L, null, null, null, null, null, 0, 20, false, Set.of("vacation"));
        AssetFilter filterFamily = new AssetFilter(1L, null, null, null, null, null, 0, 20, false, Set.of("family"));

        Object keyVacation = sut.generate(null, null, filterVacation);
        Object keyFamily = sut.generate(null, null, filterFamily);

        assertThat(keyVacation).isNotEqualTo(keyFamily);
    }

    @Test
    void generate_tagOrderDoesNotAffectKey() {
        Set<String> tagsAB = new LinkedHashSet<>(Set.of("a"));
        tagsAB.add("b");
        Set<String> tagsBA = new LinkedHashSet<>(Set.of("b"));
        tagsBA.add("a");
        AssetFilter filterAB = new AssetFilter(1L, null, null, null, null, null, 0, 20, false, tagsAB);
        AssetFilter filterBA = new AssetFilter(1L, null, null, null, null, null, 0, 20, false, tagsBA);

        Object keyAB = sut.generate(null, null, filterAB);
        Object keyBA = sut.generate(null, null, filterBA);

        assertThat(keyAB).isEqualTo(keyBA);
    }

    @Test
    void generate_nullFolderId_handledWithoutThrowing() {
        AssetFilter filter = new AssetFilter(null, null, null, null, null, null, 0, 20, false, null);

        assertThatCode(() -> sut.generate(null, null, filter)).doesNotThrowAnyException();
        Object key = sut.generate(null, null, filter);
        assertThat(key.toString()).startsWith("none:");
    }
}
