package com.jpablodrexler.photomanager.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThumbnailStorageServiceAdapterTest {

    @Mock
    RedisTemplate<String, byte[]> thumbnailRedisTemplate;

    @Mock
    ValueOperations<String, byte[]> valueOperations;

    ThumbnailStorageServiceAdapter sut;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        lenient().when(thumbnailRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // The stubbing above is itself an interaction with the mock; clear it so tests that
        // assert "no Redis interaction at all" (cache disabled) aren't tripped up by setup noise.
        clearInvocations(thumbnailRedisTemplate);

        sut = new ThumbnailStorageServiceAdapter(thumbnailRedisTemplate);
        ReflectionTestUtils.setField(sut, "thumbnailsDirectory", tempDir.toString());
        ReflectionTestUtils.setField(sut, "thumbnailCacheEnabled", true);
        ReflectionTestUtils.setField(sut, "thumbnailCacheTtlSeconds", 86400L);
    }

    @Test
    void saveThumbnail_validData_writesCorrectBytesToDisk() throws Exception {
        byte[] data = new byte[]{10, 20, 30};

        sut.saveThumbnail("1.bin", data);

        assertThat(Files.readAllBytes(tempDir.resolve("1.bin"))).isEqualTo(data);
    }

    @Test
    void loadThumbnail_missingBlob_returnsNull() {
        byte[] result = sut.loadThumbnail("missing.bin");

        assertThat(result).isNull();
    }

    @Test
    void deleteThumbnail_existingBlob_removesFileFromDisk() {
        sut.saveThumbnail("3.bin", new byte[]{1});

        sut.deleteThumbnail("3.bin");

        assertThat(tempDir.resolve("3.bin")).doesNotExist();
    }

    @Test
    void deleteThumbnail_missingBlob_doesNotThrow() {
        assertThatCode(() -> sut.deleteThumbnail("nonexistent.bin"))
                .doesNotThrowAnyException();
    }

    @Test
    void thumbnailExists_existingBlob_returnsTrue() {
        sut.saveThumbnail("4.bin", new byte[]{1});

        boolean result = sut.thumbnailExists("4.bin");

        assertThat(result).isTrue();
    }

    @Test
    void thumbnailExists_missingBlob_returnsFalse() {
        boolean result = sut.thumbnailExists("missing.bin");

        assertThat(result).isFalse();
    }

    @Test
    void loadThumbnail_cacheHit_returnsCachedBytesWithoutDiskRead() throws IOException {
        byte[] cachedBytes = new byte[]{9, 9, 9};
        byte[] diskBytes = new byte[]{1, 2, 3};
        Files.write(tempDir.resolve("10.bin"), diskBytes);
        when(valueOperations.get("asset:thumbnail:10")).thenReturn(cachedBytes);

        byte[] result = sut.loadThumbnail("10.bin");

        assertThat(result).isEqualTo(cachedBytes);
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void loadThumbnail_cacheMiss_fallsBackToDiskAndPopulatesCacheWithConfiguredTtl() throws IOException {
        byte[] diskBytes = new byte[]{4, 5, 6};
        Files.write(tempDir.resolve("20.bin"), diskBytes);
        when(valueOperations.get("asset:thumbnail:20")).thenReturn(null);

        byte[] result = sut.loadThumbnail("20.bin");

        assertThat(result).isEqualTo(diskBytes);
        verify(valueOperations).set(eq("asset:thumbnail:20"), eq(diskBytes), eq(Duration.ofSeconds(86400)));
    }

    @Test
    void loadThumbnail_cacheMissAndDiskMiss_returnsNullAndWritesNothingToCache() {
        when(valueOperations.get("asset:thumbnail:30")).thenReturn(null);

        byte[] result = sut.loadThumbnail("30.bin");

        assertThat(result).isNull();
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void saveThumbnail_validData_writesToDiskAndRedisCacheWithConfiguredTtl() throws IOException {
        byte[] data = new byte[]{7, 8, 9};

        sut.saveThumbnail("40.bin", data);

        assertThat(Files.readAllBytes(tempDir.resolve("40.bin"))).isEqualTo(data);
        verify(valueOperations).set(eq("asset:thumbnail:40"), eq(data), eq(Duration.ofSeconds(86400)));
    }

    @Test
    void saveThumbnail_diskWriteFails_doesNotPopulateCache() throws IOException {
        // Force Files.createDirectories(thumbnailsDirectory) to throw by pointing the directory
        // at a path that is already occupied by a regular file.
        Path blockedDir = tempDir.resolve("blocked");
        Files.write(blockedDir, new byte[]{0});
        ReflectionTestUtils.setField(sut, "thumbnailsDirectory", blockedDir.toString());

        sut.saveThumbnail("110.bin", new byte[]{1, 2, 3});

        verifyNoInteractions(thumbnailRedisTemplate);
    }

    @Test
    void deleteThumbnail_existingCacheEntry_deletesFileAndEvictsCacheEntry() throws IOException {
        Files.write(tempDir.resolve("50.bin"), new byte[]{1});

        sut.deleteThumbnail("50.bin");

        assertThat(tempDir.resolve("50.bin")).doesNotExist();
        verify(thumbnailRedisTemplate).delete("asset:thumbnail:50");
    }

    @Test
    void deleteThumbnail_noCacheEntry_doesNotThrow() {
        when(thumbnailRedisTemplate.delete("asset:thumbnail:60")).thenReturn(false);

        assertThatCode(() -> sut.deleteThumbnail("60.bin")).doesNotThrowAnyException();
    }

    @Test
    void loadThumbnail_redisGetThrowsDataAccessException_fallsBackToDiskWithoutThrowing() throws IOException {
        byte[] diskBytes = new byte[]{1, 1, 1};
        Files.write(tempDir.resolve("70.bin"), diskBytes);
        when(valueOperations.get("asset:thumbnail:70")).thenThrow(new RedisConnectionFailureException("down"));

        byte[] result = sut.loadThumbnail("70.bin");

        assertThat(result).isEqualTo(diskBytes);
    }

    @Test
    void saveThumbnail_redisSetThrowsDataAccessException_stillWritesToDiskWithoutThrowing() {
        byte[] data = new byte[]{2, 2, 2};
        doThrow(new RedisConnectionFailureException("down"))
                .when(valueOperations).set(eq("asset:thumbnail:80"), any(byte[].class), any(Duration.class));

        assertThatCode(() -> sut.saveThumbnail("80.bin", data)).doesNotThrowAnyException();

        assertThat(tempDir.resolve("80.bin")).exists();
    }

    @Test
    void deleteThumbnail_redisDeleteThrowsDataAccessException_stillDeletesFromDiskWithoutThrowing() throws IOException {
        Files.write(tempDir.resolve("90.bin"), new byte[]{3});
        when(thumbnailRedisTemplate.delete("asset:thumbnail:90")).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> sut.deleteThumbnail("90.bin")).doesNotThrowAnyException();

        assertThat(tempDir.resolve("90.bin")).doesNotExist();
    }

    @Test
    void saveThumbnail_cacheDisabled_skipsRedisInteraction() {
        ReflectionTestUtils.setField(sut, "thumbnailCacheEnabled", false);
        byte[] data = new byte[]{5, 5, 5};

        sut.saveThumbnail("101.bin", data);

        verifyNoInteractions(thumbnailRedisTemplate);
    }

    @Test
    void loadThumbnail_cacheDisabled_readsFromDiskAndSkipsRedisInteraction() throws IOException {
        ReflectionTestUtils.setField(sut, "thumbnailCacheEnabled", false);
        byte[] data = new byte[]{5, 5, 5};
        Files.write(tempDir.resolve("100.bin"), data);

        byte[] loaded = sut.loadThumbnail("100.bin");

        assertThat(loaded).isEqualTo(data);
        verifyNoInteractions(thumbnailRedisTemplate);
    }

    @Test
    void deleteThumbnail_cacheDisabled_skipsRedisInteraction() throws IOException {
        ReflectionTestUtils.setField(sut, "thumbnailCacheEnabled", false);
        Files.write(tempDir.resolve("100.bin"), new byte[]{5, 5, 5});

        sut.deleteThumbnail("100.bin");

        verifyNoInteractions(thumbnailRedisTemplate);
    }
}
