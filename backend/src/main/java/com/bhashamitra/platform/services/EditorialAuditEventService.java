package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.EditorialAuditEvent;
import com.bhashamitra.platform.repositories.EditorialAuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class EditorialAuditEventService {

    private final EditorialAuditEventRepository repository;

    public EditorialAuditEventService(EditorialAuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Append-only audit event write.
     * Always inserts a new row (never updates).
     */
    @Transactional
    public EditorialAuditEvent recordEvent(
            String entityType,
            String entityId,
            String eventType,
            String actor,
            String comment,
            String details
    ) {
        EditorialAuditEvent e = new EditorialAuditEvent();

        e.setEntityType(requireNonBlank(entityType, "entityType"));
        e.setEntityId(requireNonBlank(entityId, "entityId"));
        e.setEventType(requireNonBlank(eventType, "eventType"));
        e.setActor(requireNonBlank(actor, "actor"));
        e.setComment(blankToNull(comment));
        e.setDetails(blankToNull(details));

        // UTC everywhere
        e.setEventTs(ZonedDateTime.now(ZoneOffset.UTC));

        // Base audit fields (use actor for now)
        e.setCreatedBy(actor);
        e.setLastModifiedBy(actor);

        return repository.save(e);
    }

    /**
     * Timeline for a given entity (paged, newest first).
     */
    @Transactional(readOnly = true)
    public Page<EditorialAuditEvent> getTimeline(String entityType, String entityId, Pageable pageable) {
        return repository.findByEntityTypeAndEntityIdOrderByEventTsDesc(
                requireNonBlank(entityType, "entityType"),
                requireNonBlank(entityId, "entityId"),
                pageable
        );
    }

    /**
     * Latest event for a given entity (useful to derive current state).
     */
    @Transactional(readOnly = true)
    public Optional<EditorialAuditEvent> getLatestEvent(String entityType, String entityId) {
        return repository.findFirstByEntityTypeAndEntityIdOrderByEventTsDesc(
                requireNonBlank(entityType, "entityType"),
                requireNonBlank(entityId, "entityId")
        );
    }

    /**
     * Recent activity filtered by event type + time window (paged, newest first).
     */
    @Transactional(readOnly = true)
    public Page<EditorialAuditEvent> getActivityByEventType(
            String eventType,
            ZonedDateTime fromUtc,
            ZonedDateTime toUtc,
            Pageable pageable
    ) {
        return repository.findByEventTypeAndEventTsBetweenOrderByEventTsDesc(
                requireNonBlank(eventType, "eventType"),
                requireNonNull(fromUtc, "fromUtc"),
                requireNonNull(toUtc, "toUtc"),
                pageable
        );
    }

    /**
     * Recent activity by actor + time window (paged, newest first).
     */
    @Transactional(readOnly = true)
    public Page<EditorialAuditEvent> getActivityByActor(
            String actor,
            ZonedDateTime fromUtc,
            ZonedDateTime toUtc,
            Pageable pageable
    ) {
        return repository.findByActorAndEventTsBetweenOrderByEventTsDesc(
                requireNonBlank(actor, "actor"),
                requireNonNull(fromUtc, "fromUtc"),
                requireNonNull(toUtc, "toUtc"),
                pageable
        );
    }

    // ---------------- helpers ----------------

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must be provided");
        }
        return v.trim();
    }

    private static <T> T requireNonNull(T v, String field) {
        if (v == null) {
            throw new IllegalArgumentException(field + " must be provided");
        }
        return v;
    }

    private static String blankToNull(String v) {
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}
