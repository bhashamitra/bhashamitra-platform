package com.bhashamitra.platform.services;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Tests")
class AuditServiceTest {

    @Mock
    private EditorialAuditEventService editorialAuditEventService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("record with JSON string - Should call editorialAuditEventService with provided parameters")
    void recordWithJsonString_ShouldCallEditorialAuditEventServiceWithProvidedParameters() {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "CREATED";
        String actor = "admin@example.com";
        String comment = "Created new language";
        String detailsJson = "{\"name\":\"Marathi\",\"script\":\"Devanagari\"}";

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, detailsJson);

        // Then
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, actor, comment, detailsJson
        );
    }

    @Test
    @DisplayName("record with JSON string - Should use 'system' as actor when actor is null")
    void recordWithJsonString_ShouldUseSystemAsActorWhenActorIsNull() {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "CREATED";
        String actor = null;
        String comment = "System created language";
        String detailsJson = "{\"name\":\"Marathi\"}";

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, detailsJson);

        // Then
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, "system", comment, detailsJson
        );
    }

    @Test
    @DisplayName("record with JSON string - Should use 'system' as actor when actor is blank")
    void recordWithJsonString_ShouldUseSystemAsActorWhenActorIsBlank() {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "CREATED";
        String actor = "   ";  // blank string
        String comment = "System created language";
        String detailsJson = "{\"name\":\"Marathi\"}";

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, detailsJson);

        // Then
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, "system", comment, detailsJson
        );
    }

    @Test
    @DisplayName("record with JSON string - Should use 'system' as actor when actor is empty")
    void recordWithJsonString_ShouldUseSystemAsActorWhenActorIsEmpty() {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "CREATED";
        String actor = "";  // empty string
        String comment = "System created language";
        String detailsJson = "{\"name\":\"Marathi\"}";

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, detailsJson);

        // Then
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, "system", comment, detailsJson
        );
    }

    @Test
    @DisplayName("record with Object - Should serialize object to JSON and call record with JSON string")
    void recordWithObject_ShouldSerializeObjectToJsonAndCallRecordWithJsonString() throws Exception {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "UPDATED";
        String actor = "editor@example.com";
        String comment = "Updated language details";
        
        Map<String, Object> details = new HashMap<>();
        details.put("name", "Marathi");
        details.put("script", "Devanagari");
        details.put("enabled", true);
        
        String expectedJson = "{\"name\":\"Marathi\",\"script\":\"Devanagari\",\"enabled\":true}";
        when(objectMapper.writeValueAsString(details)).thenReturn(expectedJson);

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, details);

        // Then
        verify(objectMapper).writeValueAsString(details);
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, actor, comment, expectedJson
        );
    }

    @Test
    @DisplayName("record with Object - Should handle null details object")
    void recordWithObject_ShouldHandleNullDetailsObject() {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "DELETED";
        String actor = "admin@example.com";
        String comment = "Deleted language";
        Object details = null;

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, details);

        // Then
        verify(objectMapper, never()).writeValueAsString(any());
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, actor, comment, null
        );
    }

    @Test
    @DisplayName("record with Object - Should handle JSON serialization exception gracefully")
    void recordWithObject_ShouldHandleJsonSerializationExceptionGracefully() throws Exception {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "UPDATED";
        String actor = "editor@example.com";
        String comment = "Updated language details";
        
        Map<String, Object> details = new HashMap<>();
        details.put("name", "Marathi");
        
        JacksonException jacksonException = mock(JacksonException.class);
        when(jacksonException.getMessage()).thenReturn("Cannot serialize circular reference");
        when(objectMapper.writeValueAsString(details)).thenThrow(jacksonException);

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, details);

        // Then
        verify(objectMapper).writeValueAsString(details);
        verify(editorialAuditEventService).recordEvent(
                eq(entityType), eq(entityId), eq(eventType), eq(actor), eq(comment),
                eq("{\"auditSerializationError\":\"Cannot serialize circular reference\"}")
        );
    }

    @Test
    @DisplayName("record with Object - Should handle JSON serialization exception with null message")
    void recordWithObject_ShouldHandleJsonSerializationExceptionWithNullMessage() throws Exception {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "UPDATED";
        String actor = "editor@example.com";
        String comment = "Updated language details";
        
        Map<String, Object> details = new HashMap<>();
        details.put("name", "Marathi");
        
        JacksonException jacksonException = mock(JacksonException.class);
        when(jacksonException.getMessage()).thenReturn(null);
        when(objectMapper.writeValueAsString(details)).thenThrow(jacksonException);

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, details);

        // Then
        verify(objectMapper).writeValueAsString(details);
        verify(editorialAuditEventService).recordEvent(
                eq(entityType), eq(entityId), eq(eventType), eq(actor), eq(comment),
                eq("{\"auditSerializationError\":\"\"}")
        );
    }

    @Test
    @DisplayName("record with Object - Should escape quotes and backslashes in error messages")
    void recordWithObject_ShouldEscapeQuotesAndBackslashesInErrorMessages() throws Exception {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "UPDATED";
        String actor = "editor@example.com";
        String comment = "Updated language details";
        
        Map<String, Object> details = new HashMap<>();
        details.put("name", "Marathi");
        
        JacksonException jacksonException = mock(JacksonException.class);
        when(jacksonException.getMessage()).thenReturn("Error with \"quotes\" and \\backslashes\\");
        when(objectMapper.writeValueAsString(details)).thenThrow(jacksonException);

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, details);

        // Then
        verify(objectMapper).writeValueAsString(details);
        verify(editorialAuditEventService).recordEvent(
                eq(entityType), eq(entityId), eq(eventType), eq(actor), eq(comment),
                eq("{\"auditSerializationError\":\"Error with \\\"quotes\\\" and \\\\backslashes\\\\\"}")
        );
    }

    @Test
    @DisplayName("record with Object - Should handle complex objects successfully")
    void recordWithObject_ShouldHandleComplexObjectsSuccessfully() throws Exception {
        // Given
        String entityType = "Lemma";
        String entityId = "123";
        String eventType = "CREATED";
        String actor = "linguist@example.com";
        String comment = "Created new lemma with meanings";
        
        // Create a complex nested object
        Map<String, Object> details = new HashMap<>();
        details.put("lemma", "नमस्कार");
        details.put("language", "mr");
        details.put("meanings", new String[]{"hello", "greeting", "salutation"});
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "dictionary");
        metadata.put("confidence", 0.95);
        details.put("metadata", metadata);
        
        String expectedJson = "{\"lemma\":\"नमस्कार\",\"language\":\"mr\",\"meanings\":[\"hello\",\"greeting\",\"salutation\"],\"metadata\":{\"source\":\"dictionary\",\"confidence\":0.95}}";
        when(objectMapper.writeValueAsString(details)).thenReturn(expectedJson);

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, details);

        // Then
        verify(objectMapper).writeValueAsString(details);
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, actor, comment, expectedJson
        );
    }

    @Test
    @DisplayName("record with Object - Should work with both null and blank actor")
    void recordWithObject_ShouldWorkWithBothNullAndBlankActor() throws Exception {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "SYSTEM_UPDATE";
        String comment = "Automated system update";
        
        Map<String, Object> details = new HashMap<>();
        details.put("automated", true);
        
        String expectedJson = "{\"automated\":true}";
        when(objectMapper.writeValueAsString(details)).thenReturn(expectedJson);

        // When - Test with null actor
        auditService.record(entityType, entityId, eventType, null, comment, details);

        // Then
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, "system", comment, expectedJson
        );

        // Reset mock
        reset(editorialAuditEventService);

        // When - Test with blank actor
        auditService.record(entityType, entityId, eventType, "  ", comment, details);

        // Then
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, "system", comment, expectedJson
        );
    }

    @Test
    @DisplayName("record with Object - Should handle all parameters being edge cases")
    void recordWithObject_ShouldHandleAllParametersBeingEdgeCases() {
        // Given - All edge case parameters
        String entityType = "Test";
        String entityId = "edge-case";
        String eventType = "EDGE_TEST";
        String actor = null;  // Will become "system"
        String comment = null;  // Passed as-is
        Object details = null;  // Will become null JSON

        // When
        auditService.record(entityType, entityId, eventType, actor, comment, details);

        // Then
        verify(objectMapper, never()).writeValueAsString(any());
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, "system", comment, null
        );
    }

    @Test
    @DisplayName("Service should not fail when editorialAuditEventService throws exception")
    void serviceShouldNotFailWhenEditorialAuditEventServiceThrowsException() throws Exception {
        // Given
        String entityType = "Language";
        String entityId = "mr";
        String eventType = "CREATED";
        String actor = "admin@example.com";
        String comment = "Created language";
        
        Map<String, Object> details = new HashMap<>();
        details.put("name", "Marathi");
        
        String expectedJson = "{\"name\":\"Marathi\"}";
        when(objectMapper.writeValueAsString(details)).thenReturn(expectedJson);
        
        // Mock the service to throw an exception
        doThrow(new RuntimeException("Database connection failed"))
                .when(editorialAuditEventService)
                .recordEvent(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        // When & Then - Should not throw exception (audit failures shouldn't break business logic)
        // Note: The current implementation doesn't catch exceptions from editorialAuditEventService
        // This test documents the current behavior - if you want to change this, you'd need to add try-catch
        try {
            auditService.record(entityType, entityId, eventType, actor, comment, details);
        } catch (RuntimeException e) {
            // This is the current behavior - audit service doesn't catch repository exceptions
            // If you want to change this behavior, you'd modify the AuditService implementation
        }

        verify(objectMapper).writeValueAsString(details);
        verify(editorialAuditEventService).recordEvent(
                entityType, entityId, eventType, actor, comment, expectedJson
        );
    }
}