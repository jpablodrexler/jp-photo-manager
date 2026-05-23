package com.jpablodrexler.photomanager.infrastructure.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodingHealthIndicatorTest {

    @Test
    void health_unreachableUrl_returnsDegraded() {
        // Use a URL that will always fail (unreachable host)
        GeocodingHealthIndicator sut = new GeocodingHealthIndicator("http://localhost:19999/unreachable");

        Health result = sut.health();

        assertThat(result.getStatus()).isEqualTo(GeocodingHealthIndicator.DEGRADED);
    }

    @Test
    void health_invalidUrl_returnsDegraded() {
        GeocodingHealthIndicator sut = new GeocodingHealthIndicator("http://this.host.does.not.exist.invalid");

        Health result = sut.health();

        assertThat(result.getStatus()).isEqualTo(GeocodingHealthIndicator.DEGRADED);
    }
}
