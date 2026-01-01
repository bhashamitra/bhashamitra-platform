package com.bhashamitra.platform.repositories;

import com.bhashamitra.platform.models.EditorialAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface EditorialAuditEventRepository extends JpaRepository<EditorialAuditEvent, String> {

    // Timeline for a specific entity (most recent first)
    Page<EditorialAuditEvent> findByEntityTypeAndEntityIdOrderByEventTsDesc(
            String entityType,
            String entityId,
            Pageable pageable
    );

    // Latest event for an entity (useful to derive current status)
    Optional<EditorialAuditEvent> findFirstByEntityTypeAndEntityIdOrderByEventTsDesc(
            String entityType,
            String entityId
    );

    // Recent activity filtered by event type + time window
    Page<EditorialAuditEvent> findByEventTypeAndEventTsBetweenOrderByEventTsDesc(
            String eventType,
            ZonedDateTime from,
            ZonedDateTime to,
            Pageable pageable
    );

    // Recent activity by actor + time window
    Page<EditorialAuditEvent> findByActorAndEventTsBetweenOrderByEventTsDesc(
            String actor,
            ZonedDateTime from,
            ZonedDateTime to,
            Pageable pageable
    );
}
