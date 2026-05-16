package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaRefreshTokenRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.RefreshTokenEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpa;
    private final RefreshTokenEntityMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return jpa.findByToken(token).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        return mapper.toDomain(jpa.save(mapper.toEntity(token)));
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpa.deleteByUser_Id(userId);
    }

    @Override
    @Transactional
    public void deleteById(Long tokenId) {
        jpa.deleteById(tokenId);
    }
}
