package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AuditLogFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.enums.AuditAction;
import com.jpablodrexler.photomanager.domain.enums.AuditEntityType;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.infrastructure.persistence.document.AuditLogDocument;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AuditLogDocumentMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.mongo.MongoAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogRepositoryImplTest {

    @Mock MongoAuditLogRepository mongoRepository;
    @Mock MongoTemplate mongoTemplate;
    @Mock AuditLogDocumentMapper mapper;
    @InjectMocks AuditLogRepositoryImpl sut;

    @Test
    void log_delegatesToMongoRepositoryWithMappedDocument() {
        AuditEvent event = AuditEvent.builder()
                .userId(UUID.randomUUID())
                .action(AuditAction.ASSET_RATED)
                .entityType(AuditEntityType.ASSET)
                .entityId("1")
                .timestamp(Instant.now())
                .build();
        AuditLogDocument document = new AuditLogDocument();
        when(mapper.toDocument(event)).thenReturn(document);

        sut.log(event);

        verify(mongoRepository).save(document);
    }

    @Test
    void findByFilters_delegatesToMongoTemplateAndMapsResults() {
        UUID userId = UUID.randomUUID();
        AuditLogFilter filter = new AuditLogFilter(userId, null, null, null, 0, 10);
        AuditLogDocument document = new AuditLogDocument();
        AuditEvent event = AuditEvent.builder().userId(userId).build();

        when(mongoTemplate.count(any(Query.class), eq(AuditLogDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(AuditLogDocument.class))).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(event);

        PaginatedResult<AuditEvent> result = sut.findByFilters(filter);

        assertThat(result.items()).containsExactly(event);
        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(10);
    }

    @Test
    void findByFilters_noFilters_buildsUnrestrictedQuery() {
        AuditLogFilter filter = new AuditLogFilter(null, null, null, null, 0, 50);
        when(mongoTemplate.count(any(Query.class), eq(AuditLogDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(AuditLogDocument.class))).thenReturn(List.of());

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        sut.findByFilters(filter);

        verify(mongoTemplate).count(queryCaptor.capture(), eq(AuditLogDocument.class));
        assertThat(queryCaptor.getValue().getQueryObject()).isEmpty();
    }
}
