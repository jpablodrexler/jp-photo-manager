package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.RefreshTokenEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaRefreshTokenRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.RefreshTokenEntityMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.redis.RedisRefreshTokenStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryImplTest {

    @Mock JpaRefreshTokenRepository jpa;
    @Mock RefreshTokenEntityMapper mapper;
    @Mock RedisRefreshTokenStore redisRefreshTokenStore;
    @InjectMocks RefreshTokenRepositoryImpl sut;

    private RefreshToken domainToken(boolean revoked) {
        User user = User.builder().id(UUID.randomUUID()).username("alice").build();
        return RefreshToken.builder()
                .tokenId(1L)
                .user(user)
                .token("tok-value")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(revoked)
                .build();
    }

    @Test
    void save_nonRevokedToken_callsMirrorSaveOnRedisStore() {
        RefreshToken domain = domainToken(false);
        RefreshToken saved = domainToken(false);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        RefreshTokenEntity savedEntity = new RefreshTokenEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(saved);

        RefreshToken result = sut.save(domain);

        assertThat(result).isEqualTo(saved);
        verify(redisRefreshTokenStore).mirrorSave(saved);
        verify(redisRefreshTokenStore, never()).mirrorRevoke(any(), any(), any());
    }

    @Test
    void save_revokedToken_callsMirrorRevokeOnRedisStore() {
        RefreshToken domain = domainToken(true);
        RefreshToken saved = domainToken(true);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        RefreshTokenEntity savedEntity = new RefreshTokenEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(saved);

        RefreshToken result = sut.save(domain);

        assertThat(result).isEqualTo(saved);
        verify(redisRefreshTokenStore).mirrorRevoke(saved.getToken(), saved.getUser().getId(), saved.getTokenId());
        verify(redisRefreshTokenStore, never()).mirrorSave(any());
    }

    @Test
    void save_redisStoreThrowsOnMirrorSave_stillReturnsMappedDomainWithoutPropagating() {
        RefreshToken domain = domainToken(false);
        RefreshToken saved = domainToken(false);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        RefreshTokenEntity savedEntity = new RefreshTokenEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(saved);
        doThrow(new RuntimeException("unexpected redis store failure"))
                .when(redisRefreshTokenStore).mirrorSave(saved);

        RefreshToken[] result = new RefreshToken[1];
        assertThatCode(() -> result[0] = sut.save(domain)).doesNotThrowAnyException();
        assertThat(result[0]).isEqualTo(saved);
    }

    @Test
    void save_redisStoreThrowsOnMirrorRevoke_stillReturnsMappedDomainWithoutPropagating() {
        RefreshToken domain = domainToken(true);
        RefreshToken saved = domainToken(true);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        RefreshTokenEntity savedEntity = new RefreshTokenEntity();
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(saved);
        doThrow(new RuntimeException("unexpected redis store failure"))
                .when(redisRefreshTokenStore).mirrorRevoke(saved.getToken(), saved.getUser().getId(), saved.getTokenId());

        RefreshToken[] result = new RefreshToken[1];
        assertThatCode(() -> result[0] = sut.save(domain)).doesNotThrowAnyException();
        assertThat(result[0]).isEqualTo(saved);
    }

    @Test
    void findByToken_present_returnsMappedDomain() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        RefreshToken domain = domainToken(false);
        when(jpa.findByToken("tok-value")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(sut.findByToken("tok-value")).contains(domain);
    }

    @Test
    void deleteByUserId_delegatesToJpaRepository() {
        UUID userId = UUID.randomUUID();

        sut.deleteByUserId(userId);

        verify(jpa).deleteByUser_Id(userId);
    }

    @Test
    void deleteById_delegatesToJpaRepository() {
        sut.deleteById(5L);

        verify(jpa).deleteById(5L);
    }
}
