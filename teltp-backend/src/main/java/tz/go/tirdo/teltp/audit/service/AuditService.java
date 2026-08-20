package tz.go.tirdo.teltp.audit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.teltp.audit.entity.AuditLog;
import tz.go.tirdo.teltp.audit.repository.AuditLogRepository;
import tz.go.tirdo.teltp.security.CurrentUser;

/**
 * Records action-trail entries. Writes in a REQUIRES_NEW transaction so an audit write never
 * rolls back (or is rolled back by) the business transaction it documents.
 */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String targetType, String targetUuid, String detail) {
        AuditLog log = new AuditLog();
        log.setActor(CurrentUser.username().orElse("system"));
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetUuid(targetUuid);
        log.setDetail(detail);
        repo.save(log);
    }
}
