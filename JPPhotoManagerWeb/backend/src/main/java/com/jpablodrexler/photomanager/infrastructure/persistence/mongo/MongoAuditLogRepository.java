package com.jpablodrexler.photomanager.infrastructure.persistence.mongo;

import com.jpablodrexler.photomanager.infrastructure.persistence.document.AuditLogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoAuditLogRepository extends MongoRepository<AuditLogDocument, String> {
}
