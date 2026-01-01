package com.bhashamitra.platform.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;

@Entity
@Table(name = "editorial_audit_events")
@Data
@EqualsAndHashCode(callSuper = true)
public class EditorialAuditEvent extends Auditable {

    @Column(name = "entity_type", length = 30, nullable = false)
    private String entityType;

    @Column(name = "entity_id", length = 36, nullable = false)
    private String entityId;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "actor", length = 125, nullable = false)
    private String actor;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "event_ts", nullable = false)
    private ZonedDateTime eventTs;
}
