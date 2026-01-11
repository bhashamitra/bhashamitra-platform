package com.bhashamitra.platform.services;

import com.bhashamitra.platform.controllers.dto.LemmaSearchRequest;
import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.LemmaStatus;
import com.bhashamitra.platform.repositories.LemmaRepository;
import com.bhashamitra.platform.services.dto.LemmaCreateRequest;
import com.bhashamitra.platform.services.dto.LemmaUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LemmaService Tests")
class LemmaServiceTest {

    @Mock
    private LemmaRepository lemmaRepository;

    @Mock
    private LanguageService languageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private LemmaService lemmaService;

    private Lemma sampleLemma;

    @BeforeEach
    void setUp() {
        sampleLemma = new Lemma();
        sampleLemma.setLanguage("mr");
        sampleLemma.setLemmaNative("नमस्कार");
        sampleLemma.setLemmaLatin("namaskar");
        sampleLemma.setPos("noun");
        sampleLemma.setNotes("Common greeting");
        sampleLemma.setStatus(LemmaStatus.DRAFT);
        sampleLemma.setCreatedBy("admin@example.com");
        sampleLemma.setLastModifiedBy("admin@example.com");
    }

    // =========================================================
    // getById Tests
    // =========================================================

    @Test
    @DisplayName("getById - Should return lemma when found")
    void getById_ShouldReturnLemmaWhenFound() {
        // Given
        String id = "lemma-123";
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));

        // When
        Lemma result = lemmaService.getById(id);

        // Then
        assertEquals(sampleLemma, result);
        verify(lemmaRepository).findById(id);
    }

    @Test
    @DisplayName("getById - Should throw IllegalArgumentException when not found")
    void getById_ShouldThrowIllegalArgumentExceptionWhenNotFound() {
        // Given
        String id = "nonexistent";
        when(lemmaRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.getById(id)
        );
        assertEquals("Lemma not found: nonexistent", exception.getMessage());
        verify(lemmaRepository).findById(id);
    }

    // =========================================================
    // listByLanguage Tests
    // =========================================================

    @Test
    @DisplayName("listByLanguage - Should return lemmas for enabled language")
    void listByLanguage_ShouldReturnLemmasForEnabledLanguage() {
        // Given
        String language = "mr";
        List<Lemma> expectedLemmas = Arrays.asList(sampleLemma);
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(lemmaRepository.findByLanguageOrderByLemmaNativeAsc(language)).thenReturn(expectedLemmas);

        // When
        List<Lemma> result = lemmaService.listByLanguage(language);

        // Then
        assertEquals(expectedLemmas, result);
        verify(languageService).isLanguageEnabled(language);
        verify(lemmaRepository).findByLanguageOrderByLemmaNativeAsc(language);
    }

    @Test
    @DisplayName("listByLanguage - Should throw exception for disabled language")
    void listByLanguage_ShouldThrowExceptionForDisabledLanguage() {
        // Given
        String language = "disabled";
        when(languageService.isLanguageEnabled(language)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.listByLanguage(language)
        );
        assertEquals("Language is not enabled or not found: disabled", exception.getMessage());
        verify(languageService).isLanguageEnabled(language);
        verify(lemmaRepository, never()).findByLanguageOrderByLemmaNativeAsc(any());
    }

    @Test
    @DisplayName("listByLanguage - Should throw exception for null language")
    void listByLanguage_ShouldThrowExceptionForNullLanguage() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.listByLanguage(null)
        );
        assertEquals("language is required", exception.getMessage());
        verify(languageService, never()).isLanguageEnabled(any());
        verify(lemmaRepository, never()).findByLanguageOrderByLemmaNativeAsc(any());
    }

    @Test
    @DisplayName("listByLanguage - Should throw exception for blank language")
    void listByLanguage_ShouldThrowExceptionForBlankLanguage() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.listByLanguage("   ")
        );
        assertEquals("language is required", exception.getMessage());
        verify(languageService, never()).isLanguageEnabled(any());
        verify(lemmaRepository, never()).findByLanguageOrderByLemmaNativeAsc(any());
    }

    // =========================================================
    // listByLanguageAndStatus Tests
    // =========================================================

    @Test
    @DisplayName("listByLanguageAndStatus - Should return lemmas for enabled language and status")
    void listByLanguageAndStatus_ShouldReturnLemmasForEnabledLanguageAndStatus() {
        // Given
        String language = "mr";
        LemmaStatus status = LemmaStatus.PUBLISHED;
        List<Lemma> expectedLemmas = Arrays.asList(sampleLemma);
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(lemmaRepository.findByLanguageAndStatusOrderByLemmaNativeAsc(language, status)).thenReturn(expectedLemmas);

        // When
        List<Lemma> result = lemmaService.listByLanguageAndStatus(language, status);

        // Then
        assertEquals(expectedLemmas, result);
        verify(languageService).isLanguageEnabled(language);
        verify(lemmaRepository).findByLanguageAndStatusOrderByLemmaNativeAsc(language, status);
    }

    // =========================================================
    // create Tests
    // =========================================================

    @Test
    @DisplayName("create - Should create lemma with default DRAFT status")
    void create_ShouldCreateLemmaWithDefaultDraftStatus() {
        // Given
        LemmaCreateRequest request = new LemmaCreateRequest(
                "mr", "नमस्कार", "namaskar", "noun", "Common greeting", null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "नमस्कार")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        Lemma result = lemmaService.create(request, actor);

        // Then
        assertEquals(sampleLemma, result);
        
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertEquals("mr", capturedLemma.getLanguage());
        assertEquals("नमस्कार", capturedLemma.getLemmaNative());
        assertEquals("namaskar", capturedLemma.getLemmaLatin());
        assertEquals("noun", capturedLemma.getPos());
        assertEquals("Common greeting", capturedLemma.getNotes());
        assertEquals(LemmaStatus.DRAFT, capturedLemma.getStatus());
        assertEquals(actor, capturedLemma.getCreatedBy());
        assertEquals(actor, capturedLemma.getLastModifiedBy());

        verify(auditService).record(eq("LEMMA"), any(String.class), eq("LEMMA_CREATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("create - Should create lemma with specified status")
    void create_ShouldCreateLemmaWithSpecifiedStatus() {
        // Given
        LemmaCreateRequest request = new LemmaCreateRequest(
                "mr", "नमस्कार", "namaskar", "noun", "Common greeting", LemmaStatus.REVIEW
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "नमस्कार")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        lemmaService.create(request, actor);

        // Then
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertEquals(LemmaStatus.REVIEW, capturedLemma.getStatus());
    }

    @Test
    @DisplayName("create - Should normalize and trim input fields")
    void create_ShouldNormalizeAndTrimInputFields() {
        // Given
        LemmaCreateRequest request = new LemmaCreateRequest(
                "  mr  ", "  नमस्कार  ", "  namaskar  ", "  noun  ", "Common greeting", null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "नमस्कार")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        lemmaService.create(request, actor);

        // Then
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertEquals("mr", capturedLemma.getLanguage());
        assertEquals("नमस्कार", capturedLemma.getLemmaNative());
        assertEquals("namaskar", capturedLemma.getLemmaLatin());
        assertEquals("noun", capturedLemma.getPos());
    }

    @Test
    @DisplayName("create - Should convert empty strings to null for optional fields")
    void create_ShouldConvertEmptyStringsToNullForOptionalFields() {
        // Given
        LemmaCreateRequest request = new LemmaCreateRequest(
                "mr", "नमस्कार", "   ", "   ", "Common greeting", null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "नमस्कार")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        lemmaService.create(request, actor);

        // Then
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertNull(capturedLemma.getLemmaLatin());
        assertNull(capturedLemma.getPos());
    }

    @Test
    @DisplayName("create - Should throw exception for duplicate lemma")
    void create_ShouldThrowExceptionForDuplicateLemma() {
        // Given
        LemmaCreateRequest request = new LemmaCreateRequest(
                "mr", "नमस्कार", "namaskar", "noun", "Common greeting", null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "नमस्कार")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.create(request, actor)
        );
        assertEquals("Lemma already exists for language=mr lemmaNative=नमस्कार", exception.getMessage());
        verify(lemmaRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create - Should handle null actor gracefully")
    void create_ShouldHandleNullActorGracefully() {
        // Given
        LemmaCreateRequest request = new LemmaCreateRequest(
                "mr", "नमस्कार", "namaskar", "noun", "Common greeting", null
        );
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "नमस्कार")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        lemmaService.create(request, null);

        // Then
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertNull(capturedLemma.getCreatedBy());
        assertNull(capturedLemma.getLastModifiedBy());
    }

    // =========================================================
    // update Tests
    // =========================================================

    @Test
    @DisplayName("update - Should update lemma fields successfully")
    void update_ShouldUpdateLemmaFieldsSuccessfully() {
        // Given
        String id = "lemma-123";
        LemmaUpdateRequest request = new LemmaUpdateRequest(
                null, "धन्यवाद", "dhanyawad", "noun", "Thank you"
        );
        String actor = "editor@example.com";
        
        Lemma updatedLemma = new Lemma();
        updatedLemma.setLanguage("mr");
        updatedLemma.setLemmaNative("धन्यवाद");
        updatedLemma.setLemmaLatin("dhanyawad");
        updatedLemma.setPos("noun");
        updatedLemma.setNotes("Thank you");
        updatedLemma.setStatus(LemmaStatus.DRAFT);
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "धन्यवाद")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(updatedLemma);

        // When
        Lemma result = lemmaService.update(id, request, actor);

        // Then
        assertEquals(updatedLemma, result);
        
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertEquals("धन्यवाद", capturedLemma.getLemmaNative());
        assertEquals("dhanyawad", capturedLemma.getLemmaLatin());
        assertEquals("noun", capturedLemma.getPos());
        assertEquals("Thank you", capturedLemma.getNotes());
        assertEquals(actor, capturedLemma.getLastModifiedBy());

        verify(auditService).record(eq("LEMMA"), any(String.class), eq("LEMMA_UPDATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("update - Should only update non-null fields")
    void update_ShouldOnlyUpdateNonNullFields() {
        // Given
        String id = "lemma-123";
        LemmaUpdateRequest request = new LemmaUpdateRequest(
                null, null, "new-latin", null, null
        );
        String actor = "editor@example.com";
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        lemmaService.update(id, request, actor);

        // Then
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertEquals("mr", capturedLemma.getLanguage()); // unchanged
        assertEquals("नमस्कार", capturedLemma.getLemmaNative()); // unchanged
        assertEquals("new-latin", capturedLemma.getLemmaLatin()); // updated
        assertEquals("noun", capturedLemma.getPos()); // unchanged
        assertEquals("Common greeting", capturedLemma.getNotes()); // unchanged
    }

    @Test
    @DisplayName("update - Should throw exception for duplicate after update")
    void update_ShouldThrowExceptionForDuplicateAfterUpdate() {
        // Given
        String id = "lemma-123";
        LemmaUpdateRequest request = new LemmaUpdateRequest(
                null, "existing-lemma", null, null, null
        );
        String actor = "editor@example.com";
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "existing-lemma")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.update(id, request, actor)
        );
        assertEquals("Lemma already exists for language=mr lemmaNative=existing-lemma", exception.getMessage());
        verify(lemmaRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - Should not check uniqueness if lemma native unchanged")
    void update_ShouldNotCheckUniquenessIfLemmaNativeUnchanged() {
        // Given
        String id = "lemma-123";
        LemmaUpdateRequest request = new LemmaUpdateRequest(
                null, null, "new-latin", "verb", null
        );
        String actor = "editor@example.com";
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        lemmaService.update(id, request, actor);

        // Then
        verify(lemmaRepository, never()).existsByLanguageAndLemmaNative(any(), any());
        verify(lemmaRepository).save(any(Lemma.class));
    }

    // =========================================================
    // setStatus Tests
    // =========================================================

    @Test
    @DisplayName("setStatus - Should update status successfully")
    void setStatus_ShouldUpdateStatusSuccessfully() {
        // Given
        String id = "lemma-123";
        LemmaStatus newStatus = LemmaStatus.REVIEW;
        String actor = "editor@example.com";
        
        Lemma updatedLemma = new Lemma();
        updatedLemma.setStatus(newStatus);
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(updatedLemma);

        // When
        Lemma result = lemmaService.setStatus(id, newStatus, actor);

        // Then
        assertEquals(updatedLemma, result);
        
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertEquals(newStatus, capturedLemma.getStatus());
        assertEquals(actor, capturedLemma.getLastModifiedBy());

        verify(auditService).record(eq("LEMMA"), any(String.class), eq("LEMMA_STATUS_CHANGED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("setStatus - Should throw exception for null status")
    void setStatus_ShouldThrowExceptionForNullStatus() {
        // Given
        String id = "lemma-123";
        String actor = "editor@example.com";
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.setStatus(id, null, actor)
        );
        assertEquals("Status cannot be null", exception.getMessage());
        verify(lemmaRepository, never()).save(any());
    }

    @Test
    @DisplayName("setStatus - Should prevent publishing archived lemma directly")
    void setStatus_ShouldPreventPublishingArchivedLemmaDirectly() {
        // Given
        String id = "lemma-123";
        sampleLemma.setStatus(LemmaStatus.ARCHIVED);
        String actor = "editor@example.com";
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.setStatus(id, LemmaStatus.PUBLISHED, actor)
        );
        assertEquals("Cannot publish an ARCHIVED lemma. Unarchive to REVIEW first.", exception.getMessage());
        verify(lemmaRepository, never()).save(any());
    }

    @Test
    @DisplayName("setStatus - Should prevent publishing draft lemma directly")
    void setStatus_ShouldPreventPublishingDraftLemmaDirectly() {
        // Given
        String id = "lemma-123";
        sampleLemma.setStatus(LemmaStatus.DRAFT);
        String actor = "editor@example.com";
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.setStatus(id, LemmaStatus.PUBLISHED, actor)
        );
        assertEquals("Cannot publish directly from DRAFT. Move to REVIEW first.", exception.getMessage());
        verify(lemmaRepository, never()).save(any());
    }

    @Test
    @DisplayName("setStatus - Should allow valid status transitions")
    void setStatus_ShouldAllowValidStatusTransitions() {
        // Test DRAFT -> REVIEW
        String id = "lemma-123";
        String actor = "editor@example.com";
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        lemmaService.setStatus(id, LemmaStatus.REVIEW, actor);

        // Then
        verify(lemmaRepository).save(any(Lemma.class));
        
        // Reset for next test
        reset(lemmaRepository);
        sampleLemma.setStatus(LemmaStatus.REVIEW);
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // Test REVIEW -> PUBLISHED
        lemmaService.setStatus(id, LemmaStatus.PUBLISHED, actor);
        verify(lemmaRepository).save(any(Lemma.class));
    }

    // =========================================================
    // listPublishedByLanguage Tests
    // =========================================================

    @Test
    @DisplayName("listPublishedByLanguage - Should return published lemmas for enabled language")
    void listPublishedByLanguage_ShouldReturnPublishedLemmasForEnabledLanguage() {
        // Given
        String language = "mr";
        sampleLemma.setStatus(LemmaStatus.PUBLISHED);
        List<Lemma> expectedLemmas = Arrays.asList(sampleLemma);
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(lemmaRepository.findByLanguageAndStatusOrderByLemmaNativeAsc(language, LemmaStatus.PUBLISHED))
                .thenReturn(expectedLemmas);

        // When
        List<Lemma> result = lemmaService.listPublishedByLanguage(language);

        // Then
        assertEquals(expectedLemmas, result);
        verify(languageService).isLanguageEnabled(language);
        verify(lemmaRepository).findByLanguageAndStatusOrderByLemmaNativeAsc(language, LemmaStatus.PUBLISHED);
    }

    // =========================================================
    // getPublishedById Tests
    // =========================================================

    @Test
    @DisplayName("getPublishedById - Should return published lemma when found")
    void getPublishedById_ShouldReturnPublishedLemmaWhenFound() {
        // Given
        String id = "lemma-123";
        sampleLemma.setStatus(LemmaStatus.PUBLISHED);
        when(lemmaRepository.findByIdAndStatus(id, LemmaStatus.PUBLISHED)).thenReturn(Optional.of(sampleLemma));

        // When
        Lemma result = lemmaService.getPublishedById(id);

        // Then
        assertEquals(sampleLemma, result);
        verify(lemmaRepository).findByIdAndStatus(id, LemmaStatus.PUBLISHED);
    }

    @Test
    @DisplayName("getPublishedById - Should throw exception when published lemma not found")
    void getPublishedById_ShouldThrowExceptionWhenPublishedLemmaNotFound() {
        // Given
        String id = "lemma-123";
        when(lemmaRepository.findByIdAndStatus(id, LemmaStatus.PUBLISHED)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.getPublishedById(id)
        );
        assertEquals("Published lemma not found: lemma-123", exception.getMessage());
        verify(lemmaRepository).findByIdAndStatus(id, LemmaStatus.PUBLISHED);
    }

    // =========================================================
    // Edge Cases and Complex Scenarios
    // =========================================================

    @Test
    @DisplayName("listByLanguage - Should return single lemma when only one exists")
    void listByLanguage_ShouldReturnSingleLemmaWhenOnlyOneExists() {
        // Given
        String language = "mr";
        List<Lemma> singleLemma = Arrays.asList(sampleLemma);
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(lemmaRepository.findByLanguageOrderByLemmaNativeAsc(language)).thenReturn(singleLemma);

        // When
        List<Lemma> result = lemmaService.listByLanguage(language);

        // Then
        assertEquals(1, result.size());
        assertEquals(sampleLemma, result.get(0));
        verify(languageService).isLanguageEnabled(language);
        verify(lemmaRepository).findByLanguageOrderByLemmaNativeAsc(language);
    }

    @Test
    @DisplayName("listByLanguage - Should return empty list when no lemmas exist for enabled language")
    void listByLanguage_ShouldReturnEmptyListWhenNoLemmasExistForEnabledLanguage() {
        // Given
        String language = "gu"; // Gujarati - enabled but no lemmas
        List<Lemma> emptyLemmas = Arrays.asList();
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(lemmaRepository.findByLanguageOrderByLemmaNativeAsc(language)).thenReturn(emptyLemmas);

        // When
        List<Lemma> result = lemmaService.listByLanguage(language);

        // Then
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(languageService).isLanguageEnabled(language);
        verify(lemmaRepository).findByLanguageOrderByLemmaNativeAsc(language);
    }

    @Test
    @DisplayName("Multiple languages scenario - Should handle mixed enabled/disabled languages correctly")
    void multipleLanguagesScenario_ShouldHandleMixedEnabledDisabledLanguagesCorrectly() {
        // Given - Setup multiple lemmas for different languages
        Lemma marathiLemma = new Lemma();
        marathiLemma.setLanguage("mr");
        marathiLemma.setLemmaNative("नमस्कार");
        marathiLemma.setStatus(LemmaStatus.PUBLISHED);

        Lemma hindiLemma = new Lemma();
        hindiLemma.setLanguage("hi");
        hindiLemma.setLemmaNative("नमस्ते");
        hindiLemma.setStatus(LemmaStatus.PUBLISHED);

        Lemma gujaratiLemma = new Lemma();
        gujaratiLemma.setLanguage("gu");
        gujaratiLemma.setLemmaNative("નમસ્તે");
        gujaratiLemma.setStatus(LemmaStatus.PUBLISHED);

        // Setup language enablement: mr=enabled, hi=enabled, gu=disabled
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(languageService.isLanguageEnabled("hi")).thenReturn(true);
        when(languageService.isLanguageEnabled("gu")).thenReturn(false);

        // Setup repository responses
        when(lemmaRepository.findByLanguageOrderByLemmaNativeAsc("mr"))
                .thenReturn(Arrays.asList(marathiLemma));
        when(lemmaRepository.findByLanguageOrderByLemmaNativeAsc("hi"))
                .thenReturn(Arrays.asList(hindiLemma));

        // When & Then - Test enabled languages work
        List<Lemma> marathiResult = lemmaService.listByLanguage("mr");
        assertEquals(1, marathiResult.size());
        assertEquals("नमस्कार", marathiResult.get(0).getLemmaNative());

        List<Lemma> hindiResult = lemmaService.listByLanguage("hi");
        assertEquals(1, hindiResult.size());
        assertEquals("नमस्ते", hindiResult.get(0).getLemmaNative());

        // When & Then - Test disabled language throws exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.listByLanguage("gu")
        );
        assertEquals("Language is not enabled or not found: gu", exception.getMessage());

        // Verify repository was never called for disabled language
        verify(lemmaRepository, never()).findByLanguageOrderByLemmaNativeAsc("gu");
    }

    @Test
    @DisplayName("All languages disabled scenario - Should reject all requests even if lemmas exist")
    void allLanguagesDisabledScenario_ShouldRejectAllRequestsEvenIfLemmasExist() {
        // Given - All languages are disabled but lemmas exist in database
        when(languageService.isLanguageEnabled("mr")).thenReturn(false);
        when(languageService.isLanguageEnabled("hi")).thenReturn(false);
        when(languageService.isLanguageEnabled("gu")).thenReturn(false);
        when(languageService.isLanguageEnabled("ta")).thenReturn(false);

        String[] disabledLanguages = {"mr", "hi", "gu", "ta"};

        // When & Then - All languages should be rejected
        for (String language : disabledLanguages) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    lemmaService.listByLanguage(language)
            );
            assertEquals("Language is not enabled or not found: " + language, exception.getMessage());
            
            // Verify repository is never called for any disabled language
            verify(lemmaRepository, never()).findByLanguageOrderByLemmaNativeAsc(language);
        }

        // Verify language service was called for each language
        verify(languageService).isLanguageEnabled("mr");
        verify(languageService).isLanguageEnabled("hi");
        verify(languageService).isLanguageEnabled("gu");
        verify(languageService).isLanguageEnabled("ta");
    }

    @Test
    @DisplayName("listPublishedByLanguage - Should handle mixed status scenarios correctly")
    void listPublishedByLanguage_ShouldHandleMixedStatusScenariosCorrectly() {
        // Given - Multiple lemmas with different statuses for same language
        Lemma publishedLemma1 = new Lemma();
        publishedLemma1.setLemmaNative("नमस्कार");
        publishedLemma1.setStatus(LemmaStatus.PUBLISHED);

        Lemma publishedLemma2 = new Lemma();
        publishedLemma2.setLemmaNative("धन्यवाद");
        publishedLemma2.setStatus(LemmaStatus.PUBLISHED);

        // Only published lemmas should be returned
        List<Lemma> publishedLemmas = Arrays.asList(publishedLemma1, publishedLemma2);
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(lemmaRepository.findByLanguageAndStatusOrderByLemmaNativeAsc("mr", LemmaStatus.PUBLISHED))
                .thenReturn(publishedLemmas);

        // When
        List<Lemma> result = lemmaService.listPublishedByLanguage("mr");

        // Then
        assertEquals(2, result.size());
        assertEquals("नमस्कार", result.get(0).getLemmaNative());
        assertEquals("धन्यवाद", result.get(1).getLemmaNative());
        
        // Verify only published status was queried
        verify(lemmaRepository).findByLanguageAndStatusOrderByLemmaNativeAsc("mr", LemmaStatus.PUBLISHED);
        verify(lemmaRepository, never()).findByLanguageAndStatusOrderByLemmaNativeAsc(eq("mr"), eq(LemmaStatus.DRAFT));
        verify(lemmaRepository, never()).findByLanguageAndStatusOrderByLemmaNativeAsc(eq("mr"), eq(LemmaStatus.REVIEW));
    }

    @Test
    @DisplayName("create - Should handle creating lemmas for different enabled languages")
    void create_ShouldHandleCreatingLemmasForDifferentEnabledLanguages() {
        // Given - Multiple create requests for different languages
        LemmaCreateRequest marathiRequest = new LemmaCreateRequest(
                "mr", "नमस्कार", "namaskar", "noun", "Greeting", null
        );
        
        LemmaCreateRequest hindiRequest = new LemmaCreateRequest(
                "hi", "नमस्ते", "namaste", "noun", "Greeting", null
        );

        String actor = "admin@example.com";
        
        // Setup language enablement
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(languageService.isLanguageEnabled("hi")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "नमस्कार")).thenReturn(false);
        when(lemmaRepository.existsByLanguageAndLemmaNative("hi", "नमस्ते")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When - Create lemmas for both languages
        lemmaService.create(marathiRequest, actor);
        lemmaService.create(hindiRequest, actor);

        // Then - Verify both languages were validated and lemmas saved
        verify(languageService).isLanguageEnabled("mr");
        verify(languageService).isLanguageEnabled("hi");
        verify(lemmaRepository, times(2)).save(any(Lemma.class));
        verify(auditService, times(2)).record(eq("LEMMA"), any(String.class), eq("LEMMA_CREATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("create - Should reject creation for disabled language even if similar enabled language exists")
    void create_ShouldRejectCreationForDisabledLanguageEvenIfSimilarEnabledLanguageExists() {
        // Given - mr is enabled, but mr-IN (regional variant) is disabled
        LemmaCreateRequest disabledLanguageRequest = new LemmaCreateRequest(
                "mr-IN", "नमस्कार", "namaskar", "noun", "Greeting", null
        );

        String actor = "admin@example.com";
        
        // Only mock the language that will actually be checked
        when(languageService.isLanguageEnabled("mr-IN")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.create(disabledLanguageRequest, actor)
        );
        assertEquals("Language is not enabled or not found: mr-IN", exception.getMessage());
        
        verify(languageService).isLanguageEnabled("mr-IN");
        verify(languageService, never()).isLanguageEnabled("mr"); // Should not check similar language
        verify(lemmaRepository, never()).existsByLanguageAndLemmaNative(any(), any());
        verify(lemmaRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getPublishedById - Should work correctly when single published lemma exists")
    void getPublishedById_ShouldWorkCorrectlyWhenSinglePublishedLemmaExists() {
        // Given
        String id = "lemma-123";
        sampleLemma.setStatus(LemmaStatus.PUBLISHED);
        when(lemmaRepository.findByIdAndStatus(id, LemmaStatus.PUBLISHED)).thenReturn(Optional.of(sampleLemma));

        // When
        Lemma result = lemmaService.getPublishedById(id);

        // Then
        assertEquals(sampleLemma, result);
        assertEquals(LemmaStatus.PUBLISHED, result.getStatus());
        verify(lemmaRepository).findByIdAndStatus(id, LemmaStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Language validation consistency - Should apply same validation across all methods")
    void languageValidationConsistency_ShouldApplySameValidationAcrossAllMethods() {
        // Given - A disabled language
        String disabledLanguage = "disabled-lang";
        when(languageService.isLanguageEnabled(disabledLanguage)).thenReturn(false);

        // When & Then - All methods should consistently reject disabled language
        
        // Test listByLanguage
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.listByLanguage(disabledLanguage)
        );
        assertEquals("Language is not enabled or not found: disabled-lang", exception1.getMessage());

        // Test listByLanguageAndStatus
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.listByLanguageAndStatus(disabledLanguage, LemmaStatus.DRAFT)
        );
        assertEquals("Language is not enabled or not found: disabled-lang", exception2.getMessage());

        // Test listPublishedByLanguage
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.listPublishedByLanguage(disabledLanguage)
        );
        assertEquals("Language is not enabled or not found: disabled-lang", exception3.getMessage());

        // Test create
        LemmaCreateRequest createRequest = new LemmaCreateRequest(
                disabledLanguage, "Test lemma", null, null, null, null
        );
        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class, () ->
                lemmaService.create(createRequest, "actor")
        );
        assertEquals("Language is not enabled or not found: disabled-lang", exception4.getMessage());

        // Verify language service was called for each method
        verify(languageService, times(4)).isLanguageEnabled(disabledLanguage);
        
        // Verify no repository calls were made
        verify(lemmaRepository, never()).findByLanguageOrderByLemmaNativeAsc(any());
        verify(lemmaRepository, never()).findByLanguageAndStatusOrderByLemmaNativeAsc(any(), any());
        verify(lemmaRepository, never()).existsByLanguageAndLemmaNative(any(), any());
        verify(lemmaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Uniqueness validation - Should handle cross-language uniqueness correctly")
    void uniquenessValidation_ShouldHandleCrossLanguageUniquenessCorrectly() {
        // Given - Same lemma native text exists in different languages (should be allowed)
        LemmaCreateRequest marathiRequest = new LemmaCreateRequest(
                "mr", "नमस्कार", "namaskar", "noun", "Marathi greeting", null
        );
        
        LemmaCreateRequest hindiRequest = new LemmaCreateRequest(
                "hi", "नमस्कार", "namaskar", "noun", "Hindi greeting", null
        );

        String actor = "admin@example.com";
        
        // Setup - Both languages enabled, no duplicates within same language
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(languageService.isLanguageEnabled("hi")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("mr", "नमस्कार")).thenReturn(false);
        when(lemmaRepository.existsByLanguageAndLemmaNative("hi", "नमस्कार")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When - Create same lemma text for different languages
        lemmaService.create(marathiRequest, actor);
        lemmaService.create(hindiRequest, actor);

        // Then - Both should succeed (uniqueness is per language, not global)
        verify(lemmaRepository).existsByLanguageAndLemmaNative("mr", "नमस्कार");
        verify(lemmaRepository).existsByLanguageAndLemmaNative("hi", "नमस्कार");
        verify(lemmaRepository, times(2)).save(any(Lemma.class));
        verify(auditService, times(2)).record(eq("LEMMA"), any(String.class), eq("LEMMA_CREATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("create - Should handle Unicode content correctly")
    void create_ShouldHandleUnicodeContentCorrectly() {
        // Given
        LemmaCreateRequest request = new LemmaCreateRequest(
                "hi", "नमस्ते", "namaste", "अव्यय", "हिंदी में अभिवादन", null
        );
        String actor = "linguist@example.com";
        
        when(languageService.isLanguageEnabled("hi")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("hi", "नमस्ते")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        lemmaService.create(request, actor);

        // Then
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertEquals("hi", capturedLemma.getLanguage());
        assertEquals("नमस्ते", capturedLemma.getLemmaNative());
        assertEquals("namaste", capturedLemma.getLemmaLatin());
        assertEquals("अव्यय", capturedLemma.getPos());
        assertEquals("हिंदी में अभिवादन", capturedLemma.getNotes());
    }

    @Test
    @DisplayName("update - Should handle language change with uniqueness check")
    void update_ShouldHandleLanguageChangeWithUniquenessCheck() {
        // Given
        String id = "lemma-123";
        LemmaUpdateRequest request = new LemmaUpdateRequest(
                "hi", "नमस्कार", null, null, null
        );
        String actor = "editor@example.com";
        
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("hi")).thenReturn(true);
        when(lemmaRepository.existsByLanguageAndLemmaNative("hi", "नमस्कार")).thenReturn(false);
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // When
        lemmaService.update(id, request, actor);

        // Then
        verify(languageService).isLanguageEnabled("hi");
        verify(lemmaRepository).existsByLanguageAndLemmaNative("hi", "नमस्कार");
        
        ArgumentCaptor<Lemma> lemmaCaptor = ArgumentCaptor.forClass(Lemma.class);
        verify(lemmaRepository).save(lemmaCaptor.capture());
        
        Lemma capturedLemma = lemmaCaptor.getValue();
        assertEquals("hi", capturedLemma.getLanguage());
    }

    @Test
    @DisplayName("Complex workflow - Should handle complete lemma lifecycle")
    void complexWorkflow_ShouldHandleCompleteLemmaLifecycle() {
        // This test demonstrates a complete workflow from creation to publication
        String id = "lemma-123";
        String actor = "admin@example.com";
        
        // Setup for status transitions
        when(lemmaRepository.findById(id)).thenReturn(Optional.of(sampleLemma));
        when(lemmaRepository.save(any(Lemma.class))).thenReturn(sampleLemma);

        // Test DRAFT -> REVIEW transition
        lemmaService.setStatus(id, LemmaStatus.REVIEW, actor);
        
        // Update lemma status for next transition
        sampleLemma.setStatus(LemmaStatus.REVIEW);
        
        // Test REVIEW -> PUBLISHED transition
        lemmaService.setStatus(id, LemmaStatus.PUBLISHED, actor);
        
        // Verify all interactions
        verify(lemmaRepository, times(2)).save(any(Lemma.class));
        verify(auditService, times(2)).record(eq("LEMMA"), any(String.class), eq("LEMMA_STATUS_CHANGED"), eq(actor), isNull(), any(Map.class));
    }

    // =========================================================
    // searchLemmas Tests
    // =========================================================

    @Test
    @DisplayName("searchLemmas - Should return paginated results with all filters")
    void searchLemmas_ShouldReturnPaginatedResultsWithAllFilters() {
        // Given
        LemmaSearchRequest request = new LemmaSearchRequest(
                "नमस्कार", "mr", "DRAFT", "noun", 0, 20, "lemmaNative", "asc"
        );
        
        Lemma lemma1 = new Lemma();
        lemma1.setLanguage("mr");
        lemma1.setLemmaNative("नमस्कार");
        lemma1.setLemmaLatin("namaskar");
        lemma1.setPos("noun");
        lemma1.setStatus(LemmaStatus.DRAFT);
        
        Page<Lemma> mockPage = mock(Page.class);
        when(mockPage.getContent()).thenReturn(Arrays.asList(lemma1));
        
        when(lemmaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        Page<Lemma> result = lemmaService.searchLemmas(request);

        // Then
        assertEquals(mockPage, result);
        assertEquals(1, result.getContent().size());
        assertEquals("नमस्कार", result.getContent().get(0).getLemmaNative());
        
        verify(lemmaRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("searchLemmas - Should handle pagination parameters")
    void searchLemmas_ShouldHandlePaginationParameters() {
        // Given
        LemmaSearchRequest request = new LemmaSearchRequest(
                null, "mr", null, null, 2, 50, "lemmaNative", "asc"
        );
        
        Page<Lemma> mockPage = mock(Page.class);
        when(lemmaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        lemmaService.searchLemmas(request);

        // Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(lemmaRepository).findAll(any(Specification.class), pageableCaptor.capture());
        
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "lemmaNative"), pageable.getSort());
    }

    @Test
    @DisplayName("searchLemmas - Should handle descending sort")
    void searchLemmas_ShouldHandleDescendingSort() {
        // Given
        LemmaSearchRequest request = new LemmaSearchRequest(
                null, "mr", null, null, 0, 20, "status", "desc"
        );
        
        Page<Lemma> mockPage = mock(Page.class);
        when(lemmaRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        lemmaService.searchLemmas(request);

        // Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(lemmaRepository).findAll(any(Specification.class), pageableCaptor.capture());
        
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(Sort.by(Sort.Direction.DESC, "status"), pageable.getSort());
    }
}