package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.EditorialAuditEvent;
import com.bhashamitra.platform.repositories.EditorialAuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EditorialAuditEventService Tests")
class EditorialAuditEventServiceTest {

    @Mock
    private EditorialAuditEventRepository repository;

    @InjectMocks
    private EditorialAuditEventService service;

    private EditorialAuditEvent sampleEvent;
    private ZonedDateTime fixedTime;

    @BeforeEach
    void setUp() {
        fixedTime = ZonedDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        
        sampleEvent = new EditorialAuditEvent();
        sampleEvent.setEntityType("Language");
        sampleEvent.setEntityId("mr");
        sampleEvent.setEventType("CREATED");
        sampleEvent.setActor("admin@example.com");
        sampleEvent.setComment("Created new language");
        sampleEvent.setDetails("{\"name\":\"Marathi\",\"script\":\"Devanagari\"}");
        sampleEvent.setEventTs(fixedTime);
        sampleEvent.setCreatedBy("admin@example.com");
        sampleEvent.setLastModifiedBy("admin@example.com");
    }

    @Test
    @DisplayName("recordEvent - Should create and save audit event with all required fields")
    void recordEvent_ShouldCreateAndSaveAuditEventWithAllRequiredFields() {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "CREATED";
        String actor = "admin@example.com";
        String comment = "Created new language";
        String details = "{\"name\":\"Marathi\"}";

        when(repository.save(any(EditorialAuditEvent.class))).thenReturn(sampleEvent);

        // When
        EditorialAuditEvent result = service.recordEvent(entityType, entityId, eventType, actor, comment, details);

        // Then
        ArgumentCaptor<EditorialAuditEvent> eventCaptor = ArgumentCaptor.forClass(EditorialAuditEvent.class);
        verify(repository).save(eventCaptor.capture());

        EditorialAuditEvent capturedEvent = eventCaptor.getValue();
        assertEquals(entityType, capturedEvent.getEntityType());
        assertEquals(entityId, capturedEvent.getEntityId());
        assertEquals(eventType, capturedEvent.getEventType());
        assertEquals(actor, capturedEvent.getActor());
        assertEquals(comment, capturedEvent.getComment());
        assertEquals(details, capturedEvent.getDetails());
        assertEquals(actor, capturedEvent.getCreatedBy());
        assertEquals(actor, capturedEvent.getLastModifiedBy());
        assertNotNull(capturedEvent.getEventTs());
        assertEquals(ZoneOffset.UTC, capturedEvent.getEventTs().getOffset());

        assertEquals(sampleEvent, result);
    }

    @Test
    @DisplayName("recordEvent - Should trim whitespace from required fields")
    void recordEvent_ShouldTrimWhitespaceFromRequiredFields() {
        // Given
        String entityType = "  Language  ";
        String entityId = "  mr  ";
        String eventType = "  CREATED  ";
        String actor = "  admin@example.com  ";
        String comment = "  Created new language  ";
        String details = "  {\"name\":\"Marathi\"}  ";

        when(repository.save(any(EditorialAuditEvent.class))).thenReturn(sampleEvent);

        // When
        service.recordEvent(entityType, entityId, eventType, actor, comment, details);

        // Then
        ArgumentCaptor<EditorialAuditEvent> eventCaptor = ArgumentCaptor.forClass(EditorialAuditEvent.class);
        verify(repository).save(eventCaptor.capture());

        EditorialAuditEvent capturedEvent = eventCaptor.getValue();
        assertEquals("Language", capturedEvent.getEntityType());
        assertEquals("mr", capturedEvent.getEntityId());
        assertEquals("CREATED", capturedEvent.getEventType());
        assertEquals("admin@example.com", capturedEvent.getActor());
        assertEquals("Created new language", capturedEvent.getComment());
        assertEquals("{\"name\":\"Marathi\"}", capturedEvent.getDetails());
    }

    @Test
    @DisplayName("recordEvent - Should convert blank comment to null")
    void recordEvent_ShouldConvertBlankCommentToNull() {
        // Given
        when(repository.save(any(EditorialAuditEvent.class))).thenReturn(sampleEvent);

        // When - Test with null comment
        service.recordEvent("Language", "mr", "CREATED", "admin", null, "{}");

        // Then
        ArgumentCaptor<EditorialAuditEvent> eventCaptor = ArgumentCaptor.forClass(EditorialAuditEvent.class);
        verify(repository).save(eventCaptor.capture());
        assertNull(eventCaptor.getValue().getComment());

        // Reset mock
        reset(repository);
        when(repository.save(any(EditorialAuditEvent.class))).thenReturn(sampleEvent);

        // When - Test with blank comment
        service.recordEvent("Language", "mr", "CREATED", "admin", "   ", "{}");

        // Then
        verify(repository).save(eventCaptor.capture());
        assertNull(eventCaptor.getValue().getComment());
    }

    @Test
    @DisplayName("recordEvent - Should convert blank details to null")
    void recordEvent_ShouldConvertBlankDetailsToNull() {
        // Given
        when(repository.save(any(EditorialAuditEvent.class))).thenReturn(sampleEvent);

        // When - Test with null details
        service.recordEvent("Language", "mr", "CREATED", "admin", "comment", null);

        // Then
        ArgumentCaptor<EditorialAuditEvent> eventCaptor = ArgumentCaptor.forClass(EditorialAuditEvent.class);
        verify(repository).save(eventCaptor.capture());
        assertNull(eventCaptor.getValue().getDetails());

        // Reset mock
        reset(repository);
        when(repository.save(any(EditorialAuditEvent.class))).thenReturn(sampleEvent);

        // When - Test with blank details
        service.recordEvent("Language", "mr", "CREATED", "admin", "comment", "   ");

        // Then
        verify(repository).save(eventCaptor.capture());
        assertNull(eventCaptor.getValue().getDetails());
    }

    @Test
    @DisplayName("recordEvent - Should throw IllegalArgumentException for null entityType")
    void recordEvent_ShouldThrowIllegalArgumentExceptionForNullEntityType() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.recordEvent(null, "mr", "CREATED", "admin", "comment", "details")
        );
        assertEquals("entityType must be provided", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("recordEvent - Should throw IllegalArgumentException for blank entityType")
    void recordEvent_ShouldThrowIllegalArgumentExceptionForBlankEntityType() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.recordEvent("   ", "mr", "CREATED", "admin", "comment", "details")
        );
        assertEquals("entityType must be provided", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("recordEvent - Should throw IllegalArgumentException for null entityId")
    void recordEvent_ShouldThrowIllegalArgumentExceptionForNullEntityId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.recordEvent("Language", null, "CREATED", "admin", "comment", "details")
        );
        assertEquals("entityId must be provided", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("recordEvent - Should throw IllegalArgumentException for null eventType")
    void recordEvent_ShouldThrowIllegalArgumentExceptionForNullEventType() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.recordEvent("Language", "mr", null, "admin", "comment", "details")
        );
        assertEquals("eventType must be provided", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("recordEvent - Should throw IllegalArgumentException for null actor")
    void recordEvent_ShouldThrowIllegalArgumentExceptionForNullActor() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.recordEvent("Language", "mr", "CREATED", null, "comment", "details")
        );
        assertEquals("actor must be provided", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("getTimeline - Should return paged timeline for entity")
    void getTimeline_ShouldReturnPagedTimelineForEntity() {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        Pageable pageable = PageRequest.of(0, 10);
        
        List<EditorialAuditEvent> events = Arrays.asList(sampleEvent);
        Page<EditorialAuditEvent> expectedPage = new PageImpl<>(events, pageable, 1);
        
        when(repository.findByEntityTypeAndEntityIdOrderByEventTsDesc(entityType, entityId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<EditorialAuditEvent> result = service.getTimeline(entityType, entityId, pageable);

        // Then
        assertEquals(expectedPage, result);
        assertEquals(1, result.getTotalElements());
        assertEquals(sampleEvent, result.getContent().get(0));
        verify(repository).findByEntityTypeAndEntityIdOrderByEventTsDesc(entityType, entityId, pageable);
    }

    @Test
    @DisplayName("getTimeline - Should throw IllegalArgumentException for null entityType")
    void getTimeline_ShouldThrowIllegalArgumentExceptionForNullEntityType() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.getTimeline(null, "mr", pageable)
        );
        assertEquals("entityType must be provided", exception.getMessage());
        verify(repository, never()).findByEntityTypeAndEntityIdOrderByEventTsDesc(any(), any(), any());
    }

    @Test
    @DisplayName("getLatestEvent - Should return latest event for entity")
    void getLatestEvent_ShouldReturnLatestEventForEntity() {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        Optional<EditorialAuditEvent> expectedEvent = Optional.of(sampleEvent);
        
        when(repository.findFirstByEntityTypeAndEntityIdOrderByEventTsDesc(entityType, entityId))
                .thenReturn(expectedEvent);

        // When
        Optional<EditorialAuditEvent> result = service.getLatestEvent(entityType, entityId);

        // Then
        assertEquals(expectedEvent, result);
        assertTrue(result.isPresent());
        assertEquals(sampleEvent, result.get());
        verify(repository).findFirstByEntityTypeAndEntityIdOrderByEventTsDesc(entityType, entityId);
    }

    @Test
    @DisplayName("getLatestEvent - Should return empty optional when no events exist")
    void getLatestEvent_ShouldReturnEmptyOptionalWhenNoEventsExist() {
        // Given
        String entityType = "Language";
        String entityId = "nonexistent";
        
        when(repository.findFirstByEntityTypeAndEntityIdOrderByEventTsDesc(entityType, entityId))
                .thenReturn(Optional.empty());

        // When
        Optional<EditorialAuditEvent> result = service.getLatestEvent(entityType, entityId);

        // Then
        assertFalse(result.isPresent());
        verify(repository).findFirstByEntityTypeAndEntityIdOrderByEventTsDesc(entityType, entityId);
    }

    @Test
    @DisplayName("getActivityByEventType - Should return paged activity filtered by event type and time range")
    void getActivityByEventType_ShouldReturnPagedActivityFilteredByEventTypeAndTimeRange() {
        // Given
        String eventType = "CREATED";
        ZonedDateTime fromUtc = fixedTime.minusHours(1);
        ZonedDateTime toUtc = fixedTime.plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);
        
        List<EditorialAuditEvent> events = Arrays.asList(sampleEvent);
        Page<EditorialAuditEvent> expectedPage = new PageImpl<>(events, pageable, 1);
        
        when(repository.findByEventTypeAndEventTsBetweenOrderByEventTsDesc(eventType, fromUtc, toUtc, pageable))
                .thenReturn(expectedPage);

        // When
        Page<EditorialAuditEvent> result = service.getActivityByEventType(eventType, fromUtc, toUtc, pageable);

        // Then
        assertEquals(expectedPage, result);
        assertEquals(1, result.getTotalElements());
        assertEquals(sampleEvent, result.getContent().get(0));
        verify(repository).findByEventTypeAndEventTsBetweenOrderByEventTsDesc(eventType, fromUtc, toUtc, pageable);
    }

    @Test
    @DisplayName("getActivityByEventType - Should throw IllegalArgumentException for null eventType")
    void getActivityByEventType_ShouldThrowIllegalArgumentExceptionForNullEventType() {
        // Given
        ZonedDateTime fromUtc = fixedTime.minusHours(1);
        ZonedDateTime toUtc = fixedTime.plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.getActivityByEventType(null, fromUtc, toUtc, pageable)
        );
        assertEquals("eventType must be provided", exception.getMessage());
        verify(repository, never()).findByEventTypeAndEventTsBetweenOrderByEventTsDesc(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getActivityByEventType - Should throw IllegalArgumentException for null fromUtc")
    void getActivityByEventType_ShouldThrowIllegalArgumentExceptionForNullFromUtc() {
        // Given
        String eventType = "CREATED";
        ZonedDateTime toUtc = fixedTime.plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.getActivityByEventType(eventType, null, toUtc, pageable)
        );
        assertEquals("fromUtc must be provided", exception.getMessage());
        verify(repository, never()).findByEventTypeAndEventTsBetweenOrderByEventTsDesc(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getActivityByActor - Should return paged activity filtered by actor and time range")
    void getActivityByActor_ShouldReturnPagedActivityFilteredByActorAndTimeRange() {
        // Given
        String actor = "admin@example.com";
        ZonedDateTime fromUtc = fixedTime.minusHours(1);
        ZonedDateTime toUtc = fixedTime.plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);
        
        List<EditorialAuditEvent> events = Arrays.asList(sampleEvent);
        Page<EditorialAuditEvent> expectedPage = new PageImpl<>(events, pageable, 1);
        
        when(repository.findByActorAndEventTsBetweenOrderByEventTsDesc(actor, fromUtc, toUtc, pageable))
                .thenReturn(expectedPage);

        // When
        Page<EditorialAuditEvent> result = service.getActivityByActor(actor, fromUtc, toUtc, pageable);

        // Then
        assertEquals(expectedPage, result);
        assertEquals(1, result.getTotalElements());
        assertEquals(sampleEvent, result.getContent().get(0));
        verify(repository).findByActorAndEventTsBetweenOrderByEventTsDesc(actor, fromUtc, toUtc, pageable);
    }

    @Test
    @DisplayName("getActivityByActor - Should throw IllegalArgumentException for null actor")
    void getActivityByActor_ShouldThrowIllegalArgumentExceptionForNullActor() {
        // Given
        ZonedDateTime fromUtc = fixedTime.minusHours(1);
        ZonedDateTime toUtc = fixedTime.plusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.getActivityByActor(null, fromUtc, toUtc, pageable)
        );
        assertEquals("actor must be provided", exception.getMessage());
        verify(repository, never()).findByActorAndEventTsBetweenOrderByEventTsDesc(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getActivityByActor - Should throw IllegalArgumentException for null toUtc")
    void getActivityByActor_ShouldThrowIllegalArgumentExceptionForNullToUtc() {
        // Given
        String actor = "admin@example.com";
        ZonedDateTime fromUtc = fixedTime.minusHours(1);
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.getActivityByActor(actor, fromUtc, null, pageable)
        );
        assertEquals("toUtc must be provided", exception.getMessage());
        verify(repository, never()).findByActorAndEventTsBetweenOrderByEventTsDesc(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Helper methods - Should handle edge cases correctly")
    void helperMethods_ShouldHandleEdgeCasesCorrectly() {
        // Test that helper methods work correctly through public API
        // No need to mock repository.save() since these tests throw exceptions before reaching it
        
        // When & Then - Should throw for empty entityId
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.recordEvent("Language", "", "CREATED", "admin", "comment", "details")
        );
        assertEquals("entityId must be provided", exception.getMessage());

        // When & Then - Should throw for empty eventType
        exception = assertThrows(IllegalArgumentException.class, () ->
                service.recordEvent("Language", "mr", "", "admin", "comment", "details")
        );
        assertEquals("eventType must be provided", exception.getMessage());

        // When & Then - Should throw for empty actor
        exception = assertThrows(IllegalArgumentException.class, () ->
                service.recordEvent("Language", "mr", "CREATED", "", "comment", "details")
        );
        assertEquals("actor must be provided", exception.getMessage());
    }

    @Test
    @DisplayName("recordEvent - Should handle complex real-world scenario")
    void recordEvent_ShouldHandleComplexRealWorldScenario() {
        // Given - Complex audit event with Unicode content and JSON details
        String entityType = "Lemma";
        String entityId = "lemma-123";
        String eventType = "MEANING_UPDATED";
        String actor = "linguist@bhashamitra.org";
        String comment = "Updated meanings for नमस्कार with regional variations";
        String details = "{\"lemma\":\"नमस्कार\",\"language\":\"mr\",\"meanings\":[\"hello\",\"greeting\",\"salutation\"],\"regions\":[\"Maharashtra\",\"Goa\"],\"confidence\":0.95}";

        when(repository.save(any(EditorialAuditEvent.class))).thenReturn(sampleEvent);

        // When
        EditorialAuditEvent result = service.recordEvent(entityType, entityId, eventType, actor, comment, details);

        // Then
        ArgumentCaptor<EditorialAuditEvent> eventCaptor = ArgumentCaptor.forClass(EditorialAuditEvent.class);
        verify(repository).save(eventCaptor.capture());

        EditorialAuditEvent capturedEvent = eventCaptor.getValue();
        assertEquals(entityType, capturedEvent.getEntityType());
        assertEquals(entityId, capturedEvent.getEntityId());
        assertEquals(eventType, capturedEvent.getEventType());
        assertEquals(actor, capturedEvent.getActor());
        assertEquals(comment, capturedEvent.getComment());
        assertEquals(details, capturedEvent.getDetails());
        assertEquals(actor, capturedEvent.getCreatedBy());
        assertEquals(actor, capturedEvent.getLastModifiedBy());
        assertNotNull(capturedEvent.getEventTs());
        
        // Verify UTC timezone
        assertEquals(ZoneOffset.UTC, capturedEvent.getEventTs().getOffset());
        
        assertEquals(sampleEvent, result);
    }
}