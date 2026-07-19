package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.domain.port.out.UserPreferenceRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaUserPreferenceRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.UserPreferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPreferenceRepositoryImpl implements UserPreferenceRepository {

    private final JpaUserPreferenceRepository jpa;
    private final UserPreferenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserPreference> findByUserId(UUID userId) {
        return jpa.findById(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void save(UserPreference preference) {
        jpa.save(mapper.toEntity(preference));
    }
}
