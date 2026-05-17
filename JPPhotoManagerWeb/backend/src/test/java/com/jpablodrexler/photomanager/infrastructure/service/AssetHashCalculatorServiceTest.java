package com.jpablodrexler.photomanager.infrastructure.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AssetHashCalculatorServiceTest {

    @InjectMocks
    AssetHashCalculatorAdapter sut;

    @TempDir
    Path tempDir;

    @Test
    void calculateHash_existingFile_returns64CharLowercaseHexString() throws IOException {
        Path file = tempDir.resolve("photo.jpg");
        Files.write(file, "sample image content".getBytes(StandardCharsets.UTF_8));

        String hash = sut.computeSha256(file.toString());

        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void calculateHash_sameFileTwice_returnsSameHash() throws IOException {
        Path file = tempDir.resolve("photo.jpg");
        Files.write(file, "deterministic content".getBytes(StandardCharsets.UTF_8));

        String first = sut.computeSha256(file.toString());
        String second = sut.computeSha256(file.toString());

        assertThat(first).isEqualTo(second);
    }

    @Test
    void calculateHash_differentContent_returnsDifferentHash() throws IOException {
        Path fileA = tempDir.resolve("a.jpg");
        Path fileB = tempDir.resolve("b.jpg");
        Files.write(fileA, "content A".getBytes(StandardCharsets.UTF_8));
        Files.write(fileB, "content B".getBytes(StandardCharsets.UTF_8));

        String hashA = sut.computeSha256(fileA.toString());
        String hashB = sut.computeSha256(fileB.toString());

        assertThat(hashA).isNotEqualTo(hashB);
    }

    @Test
    void calculateHash_emptyFile_returnsKnownEmptySHA256() throws IOException {
        Path file = tempDir.resolve("empty.jpg");
        Files.write(file, new byte[0]);

        String hash = sut.computeSha256(file.toString());

        assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void calculateHash_nonExistentFile_throwsIOException() {
        String missing = tempDir.resolve("missing.jpg").toString();

        assertThatThrownBy(() -> sut.computeSha256(missing))
                .isInstanceOf(IOException.class);
    }
}
