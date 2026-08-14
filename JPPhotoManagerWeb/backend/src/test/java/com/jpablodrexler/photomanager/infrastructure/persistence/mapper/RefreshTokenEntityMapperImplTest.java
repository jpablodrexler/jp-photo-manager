package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.RefreshTokenEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenEntityMapperImplTest {

    private RefreshTokenEntityMapper sut;

    @BeforeEach
    void setUp() {
        RefreshTokenEntityMapperImpl impl = new RefreshTokenEntityMapperImpl();
        ReflectionTestUtils.setField(impl, "userEntityMapper", new UserEntityMapperImpl());
        sut = impl;
    }

    @Test
    void toDomain_mapsAllFieldsIncludingNestedUser() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setUsername("alice");
        userEntity.setPasswordHash("hash");
        userEntity.setCreatedAt(createdAt);
        userEntity.setRole("USER");

        Instant expiresAt = Instant.parse("2026-02-01T00:00:00Z");
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant lastUsedAt = Instant.parse("2026-01-15T00:00:00Z");

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setTokenId(1L);
        entity.setUser(userEntity);
        entity.setToken("token-value");
        entity.setExpiresAt(expiresAt);
        entity.setRevoked(true);
        entity.setIssuedAt(issuedAt);
        entity.setUserAgent("Mozilla/5.0");
        entity.setLastUsedAt(lastUsedAt);

        RefreshToken result = sut.toDomain(entity);

        assertThat(result.getTokenId()).isEqualTo(1L);
        assertThat(result.getUser().getId()).isEqualTo(userId);
        assertThat(result.getUser().getUsername()).isEqualTo("alice");
        assertThat(result.getToken()).isEqualTo("token-value");
        assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.isRevoked()).isTrue();
        assertThat(result.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(result.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(result.getLastUsedAt()).isEqualTo(lastUsedAt);
    }

    @Test
    void toDomain_nullEntity_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void toEntity_mapsFieldsAndUsesUserRefWithOnlyId() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("bob").role("ADMIN").build();
        Instant expiresAt = Instant.parse("2026-03-01T00:00:00Z");
        Instant issuedAt = Instant.parse("2026-02-01T00:00:00Z");

        RefreshToken domain = RefreshToken.builder()
                .tokenId(2L)
                .user(user)
                .token("another-token")
                .expiresAt(expiresAt)
                .revoked(false)
                .issuedAt(issuedAt)
                .userAgent("curl/8.0")
                .build();

        RefreshTokenEntity result = sut.toEntity(domain);

        assertThat(result.getTokenId()).isEqualTo(2L);
        assertThat(result.getUser().getId()).isEqualTo(userId);
        assertThat(result.getUser().getUsername()).isNull();
        assertThat(result.getToken()).isEqualTo("another-token");
        assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(result.getUserAgent()).isEqualTo("curl/8.0");
    }

    @Test
    void toEntity_nullDomain_returnsNull() {
        assertThat(sut.toEntity(null)).isNull();
    }
}
