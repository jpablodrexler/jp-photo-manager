package com.jpablodrexler.photomanager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.model.Folder;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@EnabledIfDockerAvailable
class UploadAssetIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8");

    @TempDir
    static Path tempDir;

    static Path catalogFolder;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    FolderRepository folderRepository;

    @BeforeAll
    static void setUp() throws Exception {
        catalogFolder = tempDir.resolve("upload-test");
        Files.createDirectories(catalogFolder);
        Files.createDirectories(Path.of("/tmp/thumbnails"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadAsset_validJpeg_returns202AndAppearsInGetAssets() throws Exception {
        Folder folder = new Folder();
        folder.setPath(catalogFolder.toString());
        folderRepository.save(folder);

        byte[] jpegBytes;
        try (InputStream in = UploadAssetIntegrationTest.class.getClassLoader()
                .getResourceAsStream("fixtures/test-with-exif.jpg")) {
            assertThat(in).as("JPEG fixture not found").isNotNull();
            jpegBytes = in.readAllBytes();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "uploaded.jpg", "image/jpeg", jpegBytes);
        MockMultipartFile folderPathPart = new MockMultipartFile(
                "folderPath", "", MediaType.TEXT_PLAIN_VALUE,
                catalogFolder.toString().getBytes());

        // Placeholder asset is persisted and visible immediately; hashing/EXIF/thumbnail generation
        // complete asynchronously via the asset-hash-processor / asset-exif-processor /
        // asset-thumbnail-processor Kafka consumer groups (kafka-async-upload), not synchronously here.
        mockMvc.perform(multipart("/api/assets/upload")
                .file(file)
                .file(folderPathPart))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.assetId").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/assets")
                .param("folderPath", catalogFolder.toString())
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].fileName").value("uploaded.jpg"));
    }
}
