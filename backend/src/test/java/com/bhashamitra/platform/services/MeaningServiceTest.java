package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.LemmaStatus;
import com.bhashamitra.platform.models.Meaning;
import com.bhashamitra.platform.repositories.LemmaRepository;
import com.bhashamitra.platform.repositories.MeaningRepository;
import com.bhashamitra.platform.services.MeaningService.MeaningCreateRequest;
import com.bhashamitra.platform.services.MeaningService.MeaningUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeaningService Tests")
class MeaningServiceTest {

    @Mock
    private MeaningRepository meaningRepository;

    @Mock
    private LemmaRepository lemmaRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MeaningService meaningService;

    private Lemma sampleLemma;
    private Meaning sampleMeaning;

    @BeforeEach
    void setUp() {
        sampleLemma = new Lemma();
        sampleLemma.setLanguage("mr");
        sampleLemma.setLemmaNative("नमस्कार");
        sampleLemma.setLemmaLatin("namaskar");
        sampleLemma.setPos("noun");
        sampleLemma.setStatus(LemmaStatus.PUBLISHED);

        sampleMeaning = new Meaning();
        sampleMeaning.setLemma(sampleLemma);
        sampleMeaning.setMeaningLanguage("en");
        sampleMeaning.setMeaningText("greeting, salutation");
        sampleMeaning.setPriority(1);
        sampleMeaning.setCreatedBy("admin@example.com");
        sampleMeaning.setLastModifiedBy("admin@example.com");
    }

    // =========================================================
    // getById Tests
    // =========================================================

    @Test
    @DisplayName("getById - Should return meaning when found")
    void getById_ShouldReturnMeaningWhenFound() {
        // Given
        String id = "meaning-123";
        when(meaningRepository.findById(id)).thenReturn(Optional.of(sampleMeaning));

        // When
        Meaning result = meaningService.getById(id);

        // Then
        assertEquals(sampleMeaning, result);
        verify(meaningRepository).findById(id);
    }

    @Test
    @DisplayName("getById - Should throw IllegalArgumentException when not found")
    void getById_ShouldThrowIllegalArgumentExceptionWhenNotFound() {
        // Given
        String id = "nonexistent";
        when(meaningRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.getById(id)
        );
        assertEquals("Meaning not found: nonexistent", exception.getMessage());
        verify(meaningRepository).findById(id);
    }

    // =========================================================
    // listByLemmaId Tests
    // =========================================================

    @Test
    @DisplayName("listByLemmaId - Should return meanings ordered by priority and id")
    void listByLemmaId_ShouldReturnMeaningsOrderedByPriorityAndId() {
        // Given
        String lemmaId = "lemma-123";
        
        Meaning meaning1 = new Meaning();
        meaning1.setLemma(sampleLemma);
        meaning1.setMeaningLanguage("en");
        meaning1.setMeaningText("greeting");
        meaning1.setPriority(1);
        
        Meaning meaning2 = new Meaning();
        meaning2.setLemma(sampleLemma);
        meaning2.setMeaningLanguage("hi");
        meaning2.setMeaningText("अभिवादन");
        meaning2.setPriority(2);
        
        List<Meaning> expectedMeanings = Arrays.asList(meaning1, meaning2);
        when(meaningRepository.findByLemma_IdOrderByPriorityAscIdAsc(lemmaId)).thenReturn(expectedMeanings);

        // When
        List<Meaning> result = meaningService.listByLemmaId(lemmaId);

        // Then
        assertEquals(expectedMeanings, result);
        assertEquals(2, result.size());
        assertEquals("greeting", result.get(0).getMeaningText());
        assertEquals("अभिवादन", result.get(1).getMeaningText());
        verify(meaningRepository).findByLemma_IdOrderByPriorityAscIdAsc(lemmaId);
    }

    @Test
    @DisplayName("listByLemmaId - Should return empty list when no meanings found")
    void listByLemmaId_ShouldReturnEmptyListWhenNoMeaningsFound() {
        // Given
        String lemmaId = "lemma-123";
        when(meaningRepository.findByLemma_IdOrderByPriorityAscIdAsc(lemmaId)).thenReturn(Arrays.asList());

        // When
        List<Meaning> result = meaningService.listByLemmaId(lemmaId);

        // Then
        assertTrue(result.isEmpty());
        verify(meaningRepository).findByLemma_IdOrderByPriorityAscIdAsc(lemmaId);
    }

    @Test
    @DisplayName("listByLemmaId - Should throw IllegalArgumentException for null lemmaId")
    void listByLemmaId_ShouldThrowIllegalArgumentExceptionForNullLemmaId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.listByLemmaId(null)
        );
        assertEquals("lemmaId must be provided", exception.getMessage());
        verifyNoInteractions(meaningRepository);
    }

    @Test
    @DisplayName("listByLemmaId - Should throw IllegalArgumentException for blank lemmaId")
    void listByLemmaId_ShouldThrowIllegalArgumentExceptionForBlankLemmaId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.listByLemmaId("   ")
        );
        assertEquals("lemmaId must be provided", exception.getMessage());
        verifyNoInteractions(meaningRepository);
    }

    // =========================================================
    // create Tests
    // =========================================================

    @Test
    @DisplayName("create - Should create meaning successfully")
    void create_ShouldCreateMeaningSuccessfully() {
        // Given
        String lemmaId = "lemma-123";
        MeaningCreateRequest request = new MeaningCreateRequest(
                lemmaId,
                "EN",  // Test case conversion to lowercase
                "greeting, salutation",
                1
        );
        String actor = "admin@example.com";

        when(lemmaRepository.findById(lemmaId)).thenReturn(Optional.of(sampleLemma));
        when(meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(lemmaId, "en", 1)).thenReturn(false);
        when(meaningRepository.save(any(Meaning.class))).thenReturn(sampleMeaning);

        // When
        Meaning result = meaningService.create(request, actor);

        // Then
        assertEquals(sampleMeaning, result);
        
        ArgumentCaptor<Meaning> meaningCaptor = ArgumentCaptor.forClass(Meaning.class);
        verify(meaningRepository).save(meaningCaptor.capture());
        
        Meaning savedMeaning = meaningCaptor.getValue();
        assertEquals(sampleLemma, savedMeaning.getLemma());
        assertEquals("en", savedMeaning.getMeaningLanguage()); // Should be lowercase
        assertEquals("greeting, salutation", savedMeaning.getMeaningText());
        assertEquals(1, savedMeaning.getPriority());
        assertEquals(actor, savedMeaning.getCreatedBy());
        assertEquals(actor, savedMeaning.getLastModifiedBy());

        // Verify audit
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).record(
                eq("MEANING"),
                eq(sampleMeaning.getId()),
                eq("MEANING_CREATED"),
                eq(actor),
                isNull(),
                detailsCaptor.capture()
        );
        
        Map<String, Object> auditDetails = detailsCaptor.getValue();
        assertEquals(sampleLemma.getId(), auditDetails.get("lemmaId"));
        assertEquals("en", auditDetails.get("meaningLanguage"));
        assertEquals(1, auditDetails.get("priority"));
    }

    @Test
    @DisplayName("create - Should create meaning with null actor")
    void create_ShouldCreateMeaningWithNullActor() {
        // Given
        String lemmaId = "lemma-123";
        MeaningCreateRequest request = new MeaningCreateRequest(
                lemmaId,
                "en",
                "greeting",
                1
        );

        when(lemmaRepository.findById(lemmaId)).thenReturn(Optional.of(sampleLemma));
        when(meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(lemmaId, "en", 1)).thenReturn(false);
        when(meaningRepository.save(any(Meaning.class))).thenReturn(sampleMeaning);

        // When
        Meaning result = meaningService.create(request, null);

        // Then
        assertEquals(sampleMeaning, result);
        
        ArgumentCaptor<Meaning> meaningCaptor = ArgumentCaptor.forClass(Meaning.class);
        verify(meaningRepository).save(meaningCaptor.capture());
        
        Meaning savedMeaning = meaningCaptor.getValue();
        assertNull(savedMeaning.getCreatedBy());
        assertNull(savedMeaning.getLastModifiedBy());
    }

    @Test
    @DisplayName("create - Should throw IllegalArgumentException for nonexistent lemma")
    void create_ShouldThrowIllegalArgumentExceptionForNonexistentLemma() {
        // Given
        String lemmaId = "nonexistent";
        MeaningCreateRequest request = new MeaningCreateRequest(
                lemmaId,
                "en",
                "greeting",
                1
        );

        when(lemmaRepository.findById(lemmaId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.create(request, "admin@example.com")
        );
        assertEquals("Lemma not found: nonexistent", exception.getMessage());
        verify(lemmaRepository).findById(lemmaId);
        verifyNoInteractions(meaningRepository);
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("create - Should throw IllegalArgumentException for duplicate meaning")
    void create_ShouldThrowIllegalArgumentExceptionForDuplicateMeaning() {
        // Given
        String lemmaId = "lemma-123";
        MeaningCreateRequest request = new MeaningCreateRequest(
                lemmaId,
                "en",
                "greeting",
                1
        );

        when(lemmaRepository.findById(lemmaId)).thenReturn(Optional.of(sampleLemma));
        when(meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(lemmaId, "en", 1)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.create(request, "admin@example.com")
        );
        assertTrue(exception.getMessage().contains("Meaning already exists"));
        assertTrue(exception.getMessage().contains("lemmaId=" + lemmaId));
        assertTrue(exception.getMessage().contains("meaningLanguage=en"));
        assertTrue(exception.getMessage().contains("priority=1"));
        
        verify(lemmaRepository).findById(lemmaId);
        verify(meaningRepository).existsByLemma_IdAndMeaningLanguageAndPriority(lemmaId, "en", 1);
        verify(meaningRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("create - Should throw IllegalArgumentException for null lemmaId")
    void create_ShouldThrowIllegalArgumentExceptionForNullLemmaId() {
        // Given
        MeaningCreateRequest request = new MeaningCreateRequest(
                null,
                "en",
                "greeting",
                1
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.create(request, "admin@example.com")
        );
        assertEquals("lemmaId must be provided", exception.getMessage());
        verifyNoInteractions(lemmaRepository, meaningRepository, auditService);
    }

    @Test
    @DisplayName("create - Should throw IllegalArgumentException for blank meaningLanguage")
    void create_ShouldThrowIllegalArgumentExceptionForBlankMeaningLanguage() {
        // Given
        MeaningCreateRequest request = new MeaningCreateRequest(
                "lemma-123",
                "   ",
                "greeting",
                1
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.create(request, "admin@example.com")
        );
        assertEquals("meaningLanguage must be provided", exception.getMessage());
        verifyNoInteractions(lemmaRepository, meaningRepository, auditService);
    }

    @Test
    @DisplayName("create - Should throw IllegalArgumentException for null meaningText")
    void create_ShouldThrowIllegalArgumentExceptionForNullMeaningText() {
        // Given
        MeaningCreateRequest request = new MeaningCreateRequest(
                "lemma-123",
                "en",
                null,
                1
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.create(request, "admin@example.com")
        );
        assertEquals("meaningText must be provided", exception.getMessage());
        verifyNoInteractions(lemmaRepository, meaningRepository, auditService);
    }

    @Test
    @DisplayName("create - Should throw IllegalArgumentException for null priority")
    void create_ShouldThrowIllegalArgumentExceptionForNullPriority() {
        // Given
        MeaningCreateRequest request = new MeaningCreateRequest(
                "lemma-123",
                "en",
                "greeting",
                null
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.create(request, "admin@example.com")
        );
        assertEquals("priority must be provided", exception.getMessage());
        verifyNoInteractions(lemmaRepository, meaningRepository, auditService);
    }

    // =========================================================
    // update Tests
    // =========================================================

    @Test
    @DisplayName("update - Should update meaning successfully")
    void update_ShouldUpdateMeaningSuccessfully() {
        // Given
        String id = "meaning-123";
        MeaningUpdateRequest request = new MeaningUpdateRequest(
                "HI",  // Test case conversion
                "अभिवादन, नमस्कार",
                2
        );
        String actor = "editor@example.com";

        Meaning existingMeaning = new Meaning();
        existingMeaning.setLemma(sampleLemma);
        existingMeaning.setMeaningLanguage("en");
        existingMeaning.setMeaningText("greeting");
        existingMeaning.setPriority(1);

        when(meaningRepository.findById(id)).thenReturn(Optional.of(existingMeaning));
        when(meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(
                sampleLemma.getId(), "hi", 2)).thenReturn(false);
        when(meaningRepository.save(any(Meaning.class))).thenReturn(existingMeaning);

        // When
        Meaning result = meaningService.update(id, request, actor);

        // Then
        assertEquals(existingMeaning, result);
        assertEquals("hi", existingMeaning.getMeaningLanguage()); // Should be lowercase
        assertEquals("अभिवादन, नमस्कार", existingMeaning.getMeaningText());
        assertEquals(2, existingMeaning.getPriority());
        assertEquals(actor, existingMeaning.getLastModifiedBy());

        // Verify audit
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).record(
                eq("MEANING"),
                eq(existingMeaning.getId()),
                eq("MEANING_UPDATED"),
                eq(actor),
                isNull(),
                detailsCaptor.capture()
        );
        
        Map<String, Object> auditDetails = detailsCaptor.getValue();
        assertEquals(sampleLemma.getId(), auditDetails.get("lemmaId"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> before = (Map<String, Object>) auditDetails.get("before");
        assertEquals("en", before.get("meaningLanguage"));
        assertEquals("greeting", before.get("meaningText"));
        assertEquals(1, before.get("priority"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> after = (Map<String, Object>) auditDetails.get("after");
        assertEquals("hi", after.get("meaningLanguage"));
        assertEquals("अभिवादन, नमस्कार", after.get("meaningText"));
        assertEquals(2, after.get("priority"));
    }

    @Test
    @DisplayName("update - Should update only meaningText when other fields are null")
    void update_ShouldUpdateOnlyMeaningTextWhenOtherFieldsAreNull() {
        // Given
        String id = "meaning-123";
        MeaningUpdateRequest request = new MeaningUpdateRequest(
                null,
                "updated greeting text",
                null
        );
        String actor = "editor@example.com";

        Meaning existingMeaning = new Meaning();
        existingMeaning.setLemma(sampleLemma);
        existingMeaning.setMeaningLanguage("en");
        existingMeaning.setMeaningText("greeting");
        existingMeaning.setPriority(1);

        when(meaningRepository.findById(id)).thenReturn(Optional.of(existingMeaning));
        when(meaningRepository.save(any(Meaning.class))).thenReturn(existingMeaning);

        // When
        Meaning result = meaningService.update(id, request, actor);

        // Then
        assertEquals("en", result.getMeaningLanguage()); // Unchanged
        assertEquals("updated greeting text", result.getMeaningText()); // Updated
        assertEquals(1, result.getPriority()); // Unchanged
        assertEquals(actor, result.getLastModifiedBy());

        // Should not check for conflicts since language and priority didn't change
        verify(meaningRepository, never()).existsByLemma_IdAndMeaningLanguageAndPriority(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("update - Should throw IllegalArgumentException for conflict on language/priority change")
    void update_ShouldThrowIllegalArgumentExceptionForConflictOnLanguagePriorityChange() {
        // Given
        String id = "meaning-123";
        MeaningUpdateRequest request = new MeaningUpdateRequest(
                "hi",
                "अभिवादन",
                2
        );

        Meaning existingMeaning = new Meaning();
        existingMeaning.setLemma(sampleLemma);
        existingMeaning.setMeaningLanguage("en");
        existingMeaning.setMeaningText("greeting");
        existingMeaning.setPriority(1);

        when(meaningRepository.findById(id)).thenReturn(Optional.of(existingMeaning));
        when(meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(
                sampleLemma.getId(), "hi", 2)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.update(id, request, "editor@example.com")
        );
        assertTrue(exception.getMessage().contains("Another meaning already exists"));
        assertTrue(exception.getMessage().contains("meaningLanguage=hi"));
        assertTrue(exception.getMessage().contains("priority=2"));
        
        verify(meaningRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("update - Should throw IllegalArgumentException for nonexistent meaning")
    void update_ShouldThrowIllegalArgumentExceptionForNonexistentMeaning() {
        // Given
        String id = "nonexistent";
        MeaningUpdateRequest request = new MeaningUpdateRequest(
                "hi",
                "अभिवादन",
                2
        );

        when(meaningRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.update(id, request, "editor@example.com")
        );
        assertEquals("Meaning not found: nonexistent", exception.getMessage());
        verify(meaningRepository).findById(id);
        verify(meaningRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("update - Should throw IllegalArgumentException for blank meaningText")
    void update_ShouldThrowIllegalArgumentExceptionForBlankMeaningText() {
        // Given
        String id = "meaning-123";
        MeaningUpdateRequest request = new MeaningUpdateRequest(
                "hi",
                "   ",
                2
        );

        when(meaningRepository.findById(id)).thenReturn(Optional.of(sampleMeaning));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.update(id, request, "editor@example.com")
        );
        assertEquals("meaningText must be provided", exception.getMessage());
        verify(meaningRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // =========================================================
    // delete Tests
    // =========================================================

    @Test
    @DisplayName("delete - Should delete meaning successfully")
    void delete_ShouldDeleteMeaningSuccessfully() {
        // Given
        String id = "meaning-123";
        String actor = "admin@example.com";

        when(meaningRepository.findById(id)).thenReturn(Optional.of(sampleMeaning));

        // When
        meaningService.delete(id, actor);

        // Then
        verify(meaningRepository).delete(sampleMeaning);

        // Verify audit
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).record(
                eq("MEANING"),
                eq(id),
                eq("MEANING_DELETED"),
                eq(actor),
                isNull(),
                detailsCaptor.capture()
        );
        
        Map<String, Object> auditDetails = detailsCaptor.getValue();
        assertEquals(sampleLemma.getId(), auditDetails.get("lemmaId"));
        assertEquals("en", auditDetails.get("meaningLanguage"));
        assertEquals(1, auditDetails.get("priority"));
    }

    @Test
    @DisplayName("delete - Should throw IllegalArgumentException for nonexistent meaning")
    void delete_ShouldThrowIllegalArgumentExceptionForNonexistentMeaning() {
        // Given
        String id = "nonexistent";
        String actor = "admin@example.com";

        when(meaningRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.delete(id, actor)
        );
        assertEquals("Meaning not found: nonexistent", exception.getMessage());
        verify(meaningRepository).findById(id);
        verify(meaningRepository, never()).delete(any());
        verifyNoInteractions(auditService);
    }

    // =========================================================
    // Edge Cases and Complex Scenarios
    // =========================================================

    @Test
    @DisplayName("create - Should handle case insensitive language normalization")
    void create_ShouldHandleCaseInsensitiveLanguageNormalization() {
        // Given
        String lemmaId = "lemma-123";
        MeaningCreateRequest request = new MeaningCreateRequest(
                lemmaId,
                "EN",  // Uppercase should be converted to lowercase
                "greeting, salutation",
                1
        );
        String actor = "admin@example.com";

        when(lemmaRepository.findById(lemmaId)).thenReturn(Optional.of(sampleLemma));
        when(meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(lemmaId, "en", 1)).thenReturn(false);
        when(meaningRepository.save(any(Meaning.class))).thenReturn(sampleMeaning);

        // When
        Meaning result = meaningService.create(request, actor);

        // Then
        assertEquals(sampleMeaning, result);
        
        ArgumentCaptor<Meaning> meaningCaptor = ArgumentCaptor.forClass(Meaning.class);
        verify(meaningRepository).save(meaningCaptor.capture());
        
        Meaning savedMeaning = meaningCaptor.getValue();
        assertEquals("en", savedMeaning.getMeaningLanguage()); // Should be lowercase
        assertEquals("greeting, salutation", savedMeaning.getMeaningText());
        assertEquals(1, savedMeaning.getPriority());
        assertEquals(actor, savedMeaning.getCreatedBy());
        assertEquals(actor, savedMeaning.getLastModifiedBy());
    }

    @Test
    @DisplayName("listByLemmaId - Should handle lemma with meanings in multiple languages")
    void listByLemmaId_ShouldHandleLemmaWithMeaningsInMultipleLanguages() {
        // Given
        String lemmaId = "lemma-123";
        
        Meaning englishMeaning = new Meaning();
        englishMeaning.setLemma(sampleLemma);
        englishMeaning.setMeaningLanguage("en");
        englishMeaning.setMeaningText("greeting");
        englishMeaning.setPriority(1);
        
        Meaning hindiMeaning = new Meaning();
        hindiMeaning.setLemma(sampleLemma);
        hindiMeaning.setMeaningLanguage("hi");
        hindiMeaning.setMeaningText("अभिवादन");
        hindiMeaning.setPriority(1);
        
        Meaning marathiMeaning = new Meaning();
        marathiMeaning.setLemma(sampleLemma);
        marathiMeaning.setMeaningLanguage("mr");
        marathiMeaning.setMeaningText("नमस्कार");
        marathiMeaning.setPriority(2);
        
        List<Meaning> expectedMeanings = Arrays.asList(englishMeaning, hindiMeaning, marathiMeaning);
        when(meaningRepository.findByLemma_IdOrderByPriorityAscIdAsc(lemmaId)).thenReturn(expectedMeanings);

        // When
        List<Meaning> result = meaningService.listByLemmaId(lemmaId);

        // Then
        assertEquals(3, result.size());
        assertEquals("en", result.get(0).getMeaningLanguage());
        assertEquals("hi", result.get(1).getMeaningLanguage());
        assertEquals("mr", result.get(2).getMeaningLanguage());
        verify(meaningRepository).findByLemma_IdOrderByPriorityAscIdAsc(lemmaId);
    }

    @Test
    @DisplayName("update - Should handle priority conflict resolution correctly")
    void update_ShouldHandlePriorityConflictResolutionCorrectly() {
        // Given - Existing meaning with priority 1
        String id = "meaning-123";
        Meaning existingMeaning = new Meaning();
        existingMeaning.setLemma(sampleLemma);
        existingMeaning.setMeaningLanguage("en");
        existingMeaning.setMeaningText("greeting");
        existingMeaning.setPriority(1);

        // Try to update to priority 2, but another meaning already has priority 2
        MeaningUpdateRequest request = new MeaningUpdateRequest(null, null, 2);

        when(meaningRepository.findById(id)).thenReturn(Optional.of(existingMeaning));
        when(meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(
                sampleLemma.getId(), "en", 2)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                meaningService.update(id, request, "editor@example.com")
        );
        assertTrue(exception.getMessage().contains("Another meaning already exists"));
        verify(meaningRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should handle Unicode content correctly")
    void create_ShouldHandleUnicodeContentCorrectly() {
        // Given
        String lemmaId = "lemma-123";
        MeaningCreateRequest request = new MeaningCreateRequest(
                lemmaId,
                "mr",
                "नमस्कार, अभिवादन, सलाम - विविध प्रकारचे शुभेच्छा",
                1
        );

        when(lemmaRepository.findById(lemmaId)).thenReturn(Optional.of(sampleLemma));
        when(meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(lemmaId, "mr", 1)).thenReturn(false);
        
        Meaning unicodeMeaning = new Meaning();
        unicodeMeaning.setLemma(sampleLemma);
        unicodeMeaning.setMeaningLanguage("mr");
        unicodeMeaning.setMeaningText("नमस्कार, अभिवादन, सलाम - विविध प्रकारचे शुभेच्छा");
        unicodeMeaning.setPriority(1);
        
        when(meaningRepository.save(any(Meaning.class))).thenReturn(unicodeMeaning);

        // When
        Meaning result = meaningService.create(request, "admin@example.com");

        // Then
        assertEquals("नमस्कार, अभिवादन, सलाम - विविध प्रकारचे शुभेच्छा", result.getMeaningText());
        assertEquals("mr", result.getMeaningLanguage());
        
        ArgumentCaptor<Meaning> meaningCaptor = ArgumentCaptor.forClass(Meaning.class);
        verify(meaningRepository).save(meaningCaptor.capture());
        assertEquals("नमस्कार, अभिवादन, सलाम - विविध प्रकारचे शुभेच्छा", 
                meaningCaptor.getValue().getMeaningText());
    }
}