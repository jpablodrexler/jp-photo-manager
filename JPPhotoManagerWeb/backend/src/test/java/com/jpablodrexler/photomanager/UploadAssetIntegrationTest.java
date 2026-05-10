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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jpablodrexler.photomanager.domain.repository.FolderRepository;
import com.jpablodrexler.photomanager.domain.entity.Folder;

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
    void uploadAsset_validJpeg_returns201AndAppearsInGetAssets() throws Exception {
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

        mockMvc.perform(multipart("/api/assets/upload")
                .file(file)
                .file(folderPathPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetId").isNumber())
                .andExpect(jsonPath("$.fileName").value("uploaded.jpg"));

        mockMvc.perform(get("/api/assets")
                .param("folderPath", catalogFolder.toString())
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].fileName").value("uploaded.jpg"));
    }
}
