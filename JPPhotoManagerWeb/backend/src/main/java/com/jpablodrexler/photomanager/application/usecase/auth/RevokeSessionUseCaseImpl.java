package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.application.exception.MissingRefreshTokenException;
import com.jpablodrexler.photomanager.application.exception.SessionNotFoundException;
import com.jpablodrexler.photomanager.application.exception.UserNotFoundException;
import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.in.auth.RevokeSessionUseCase;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RevokeSessionUseCaseImpl implements RevokeSessionUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void revokeOne(long sessionId) {
        User currentUser = currentUser();
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .filter(t -> t.getUser().getId().equals(currentUser.getId()))
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
        refreshTokenRepository.deleteById(token.getTokenId());
    }

    @Override
    @Transactional
    public void revokeAllOthers(String currentRefreshTokenValue) {
        if (currentRefreshTokenValue == null || currentRefreshTokenValue.isBlank()) {
            // Without a verifiable current session there is nothing safe to exclude from deletion.
            // Failing loudly (rather than silently no-op'ing) matters here specifically because this
            // is the "I think my account is compromised" panic button — a caller who thinks they just
            // revoked every other session, when nothing actually happened, is worse than one who sees
            // an error and knows to retry (e.g. after a fresh login re-establishes the cookie).
            throw new MissingRefreshTokenException();
        }
        User currentUser = currentUser();
        List<RefreshToken> tokens = refreshTokenRepository.findActiveByUserId(currentUser.getId(), Instant.now());
        for (RefreshToken token : tokens) {
            if (!Objects.equals(token.getToken(), currentRefreshTokenValue)) {
                refreshTokenRepository.deleteById(token.getTokenId());
            }
        }
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }
}
