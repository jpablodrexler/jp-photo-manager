package com.jpablodrexler.photomanager.infrastructure.persistence.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferenceEntityTest {

    UserPreferenceEntity sut = new UserPreferenceEntity();

    @Test
    void touch_beforePersist_setsUpdatedAtToNow() {
        sut.setUserId(UUID.randomUUID());
        sut.setThemeMode("dark");
        Instant before = Instant.now();

        sut.touch();

        assertThat(sut.getUpdatedAt()).isNotNull();
        assertThat(sut.getUpdatedAt()).isAfterOrEqualTo(before.minusSeconds(1));
    }

    @Test
    void touch_calledAgain_refreshesUpdatedAt() throws InterruptedException {
        sut.touch();
        Instant first = sut.getUpdatedAt();
        Thread.sleep(5);

        sut.touch();

        assertThat(sut.getUpdatedAt()).isAfterOrEqualTo(first);
    }
}
