// ===== FILE: ./com/bhashamitra/platform/services/AuditService.java =====
package com.bhashamitra.platform.services;


import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final EditorialAuditEventService editorialAuditEventService;
    private final ObjectMapper objectMapper;

    public AuditService(EditorialAuditEventService editorialAuditEventService, ObjectMapper objectMapper) {
        this.editorialAuditEventService = editorialAuditEventService;
        this.objectMapper = objectMapper;
    }

    public void record(String entityType,
                       String entityId,
                       String eventType,
                       String actor,
                       String comment,
                       String detailsJson) {

        String safeActor = (actor == null || actor.isBlank()) ? "system" : actor;

        editorialAuditEventService.recordEvent(
                entityType,
                entityId,
                eventType,
                safeActor,
                comment,
                detailsJson
        );
    }

    // NEW: preferred API
    public void record(String entityType,
                       String entityId,
                       String eventType,
                       String actor,
                       String comment,
                       Object details) {

        String json = toJson(details);
        record(entityType, entityId, eventType, actor, comment, json);
    }

    private String toJson(Object details) {
        if (details == null) return null;
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JacksonException e) {
            // Never block the main business operation because audit serialization failed
            return "{\"auditSerializationError\":\"" + safeMessage(e.getMessage()) + "\"}";
        }
    }

    private static String safeMessage(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
