package com.jpablodrexler.photomanager;

import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import com.jpablodrexler.photomanager.infrastructure.service.UserServiceImpl;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the {@code redis-refresh-tokens} Phase 1 dual-write mirroring: after a login, the
 * issued refresh token exists as a Redis hash with a positive TTL; after a refresh, the old
 * token's Redis mirror is gone and the new token's mirror exists. PostgreSQL remains the read
 * source of truth throughout — these assertions only inspect Redis's side-effect state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@EnabledIfDockerAvailable
class RefreshTokenRedisMirrorIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static final String USERNAME = "redis-mirror-test-user";
    private static final String PASSWORD = "password123";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserServiceImpl userService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        if (userRepository.findByUsername(USERNAME).isEmpty()) {
            userService.register(USERNAME, PASSWORD);
        }
    }

    @Test
    void login_issuesRefreshToken_mirrorsHashIntoRedisWithPositiveTtl() throws Exception {
        MvcResult loginResult = login();
        String tokenValue = extractCookie(loginResult, "refreshToken");

        String redisKey = "refresh_token:" + tokenValue;
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(redisKey);
        assertThat(hash)
                .containsKey("userId")
                .containsKey("tokenId")
                .containsKey("issuedAt");

        Long ttlSeconds = redisTemplate.getExpire(redisKey);
        assertThat(ttlSeconds).isPositive();
    }

    @Test
    void refresh_rotatesToken_removesOldMirrorAndCreatesNewMirror() throws Exception {
        MvcResult loginResult = login();
        String oldToken = extractCookie(loginResult, "refreshToken");
        assertThat(redisTemplate.hasKey("refresh_token:" + oldToken)).isTrue();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", oldToken)))
                .andExpect(status().isOk())
                .andReturn();
        String newToken = extractCookie(refreshResult, "refreshToken");

        assertThat(redisTemplate.hasKey("refresh_token:" + oldToken)).isFalse();
        assertThat(redisTemplate.hasKey("refresh_token:" + newToken)).isTrue();
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String extractCookie(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).as("cookie '%s' present in response", name).isNotNull();
        return cookie.getValue();
    }
}
