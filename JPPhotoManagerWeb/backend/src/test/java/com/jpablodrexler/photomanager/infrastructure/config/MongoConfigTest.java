package com.jpablodrexler.photomanager.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import static org.assertj.core.api.Assertions.assertThat;

class MongoConfigTest {

    MongoConfig sut = new MongoConfig();

    @Test
    void mongoCustomConversions_returnsConversionsRegisteringLegacyAuditActionConverter() {
        MongoCustomConversions conversions = sut.mongoCustomConversions();

        assertThat(conversions).isNotNull();
        assertThat(conversions.hasCustomReadTarget(String.class,
                com.jpablodrexler.photomanager.domain.enums.AuditAction.class)).isTrue();
    }
}
