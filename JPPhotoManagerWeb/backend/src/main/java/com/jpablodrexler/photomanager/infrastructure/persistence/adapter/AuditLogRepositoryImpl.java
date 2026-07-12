package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.AuditLogFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.AuditEvent;
import com.jpablodrexler.photomanager.domain.port.out.AuditLogRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.document.AuditLogDocument;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.AuditLogDocumentMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.mongo.MongoAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final MongoAuditLogRepository mongoRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditLogDocumentMapper mapper;

    @Override
    public void log(AuditEvent event) {
        mongoRepository.save(mapper.toDocument(event));
    }

    @Override
    public PaginatedResult<AuditEvent> findByFilters(AuditLogFilter filter) {
        Query query = buildFilterQuery(filter);

        long total = mongoTemplate.count(query, AuditLogDocument.class);

        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        query.with(PageRequest.of(filter.page(), filter.pageSize()));

        List<AuditEvent> items = mongoTemplate.find(query, AuditLogDocument.class).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());

        return new PaginatedResult<>(items, total, filter.page(), filter.pageSize());
    }

    private Query buildFilterQuery(AuditLogFilter filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (filter.userId() != null) {
            criteriaList.add(Criteria.where("userId").is(filter.userId()));
        }
        if (filter.entityId() != null) {
            criteriaList.add(Criteria.where("entityId").is(filter.entityId()));
        }
        if (filter.from() != null || filter.to() != null) {
            Criteria timestampCriteria = Criteria.where("timestamp");
            if (filter.from() != null) {
                timestampCriteria = timestampCriteria.gte(filter.from());
            }
            if (filter.to() != null) {
                timestampCriteria = timestampCriteria.lte(filter.to());
            }
            criteriaList.add(timestampCriteria);
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        return query;
    }
}
