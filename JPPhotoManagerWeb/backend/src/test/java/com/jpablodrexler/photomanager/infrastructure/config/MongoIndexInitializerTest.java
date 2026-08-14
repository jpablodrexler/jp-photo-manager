package com.jpablodrexler.photomanager.infrastructure.config;

import com.jpablodrexler.photomanager.infrastructure.persistence.document.AuditLogDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MongoIndexInitializerTest {

    @Mock MongoTemplate mongoTemplate;
    @Mock IndexOperations indexOperations;
    @InjectMocks MongoIndexInitializer sut;

    @Test
    void run_ensuresCompoundAndTtlIndexesOnAuditLogCollection() {
        when(mongoTemplate.indexOps(AuditLogDocument.class)).thenReturn(indexOperations);

        sut.run(null);

        ArgumentCaptor<Index> captor = ArgumentCaptor.forClass(Index.class);
        verify(indexOperations, times(2)).ensureIndex(captor.capture());

        Index compoundIndex = captor.getAllValues().get(0);
        assertThat(compoundIndex.getIndexKeys().keySet()).containsExactly("userId", "timestamp");

        Index ttlIndex = captor.getAllValues().get(1);
        assertThat(ttlIndex.getIndexKeys().keySet()).containsExactly("timestamp");
        assertThat(ttlIndex.getIndexOptions().get("expireAfterSeconds"))
                .isEqualTo(Duration.ofDays(365).toSeconds());
    }
}
