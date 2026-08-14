package com.jpablodrexler.photomanager.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SimpleStatusAggregator#getAggregateStatus} resolves ties by picking whichever supplied
 * status has the <em>lowest index</em> in the constructor's status-order list (verified against
 * {@code SimpleStatusAggregator}'s bytecode: it takes {@code Stream.min(comparator)}, and the
 * comparator ranks by {@code order.indexOf(status)} ascending) — i.e. the first-listed status
 * wins whenever it's present, not the last. Spring's own {@code DEFAULT_ORDER} lists {@code DOWN}
 * first for exactly this reason (so a {@code DOWN} indicator always wins the aggregate). This
 * bean's order is {@code "UP", "DEGRADED", "UNKNOWN", "DOWN"} — {@code UP} first — so with this
 * exact bean, {@code UP} wins the aggregate over every other status whenever at least one
 * indicator reports {@code UP}, even if another indicator reports {@code DOWN}. These tests pin
 * that actual (if perhaps unintended) behavior; changing it would be a production code change,
 * out of this test-only pass's scope.
 */
class HealthConfigTest {

    HealthConfig sut = new HealthConfig();

    @Test
    void statusAggregator_singleUpStatus_returnsUp() {
        StatusAggregator aggregator = sut.statusAggregator();

        Status result = aggregator.getAggregateStatus(Set.of(Status.UP));

        assertThat(result).isEqualTo(Status.UP);
    }

    @Test
    void statusAggregator_singleDownStatus_returnsDown() {
        StatusAggregator aggregator = sut.statusAggregator();

        Status result = aggregator.getAggregateStatus(Set.of(Status.DOWN));

        assertThat(result).isEqualTo(Status.DOWN);
    }

    @Test
    void statusAggregator_upAndDown_upWinsBecauseItIsListedFirst() {
        StatusAggregator aggregator = sut.statusAggregator();

        Status result = aggregator.getAggregateStatus(Set.of(Status.UP, Status.DOWN));

        assertThat(result).isEqualTo(Status.UP);
    }

    @Test
    void statusAggregator_degradedAndDown_degradedWinsBecauseItIsListedEarlier() {
        StatusAggregator aggregator = sut.statusAggregator();

        Status result = aggregator.getAggregateStatus(Set.of(new Status("DEGRADED"), Status.DOWN));

        assertThat(result.getCode()).isEqualTo("DEGRADED");
    }
}
