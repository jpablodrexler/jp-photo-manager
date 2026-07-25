package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.application.exception.UserNotFoundException;
import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.SessionInfo;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.auth.GetActiveSessionsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GetActiveSessionsUseCaseImpl implements GetActiveSessionsUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SessionInfo> execute(String currentRefreshTokenValue) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        Instant now = Instant.now();
        return refreshTokenRepository.findActiveByUserId(user.getId(), now).stream()
                .sorted(Comparator.comparing(GetActiveSessionsUseCaseImpl::sortKey).reversed())
                .map(token -> new SessionInfo(
                        token.getTokenId(),
                        DeviceHintParser.parse(token.getUserAgent()),
                        token.getLastUsedAt(),
                        Objects.equals(token.getToken(), currentRefreshTokenValue)))
                .toList();
    }

    private static Instant sortKey(RefreshToken token) {
        return token.getLastUsedAt() != null ? token.getLastUsedAt() : Instant.MIN;
    }
}
