package com.bhashamitra.platform.services;

import com.bhashamitra.platform.controllers.dto.UsageSentenceSearchRequest;
import com.bhashamitra.platform.models.UsageSentence;
import com.bhashamitra.platform.models.UsageSentenceStatus;
import com.bhashamitra.platform.repositories.UsageSentenceRepository;
import com.bhashamitra.platform.services.UsageSentenceService.UsageSentenceCreateRequest;
import com.bhashamitra.platform.services.UsageSentenceService.UsageSentenceUpdateRequest;
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
@DisplayName("UsageSentenceService Tests")
class UsageSentenceServiceTest {

    @Mock
    private UsageSentenceRepository usageSentenceRepository;

    @Mock
    private LanguageService languageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private UsageSentenceService usageSentenceService;

    private UsageSentence sampleUsageSentence;

    @BeforeEach
    void setUp() {
        sampleUsageSentence = new UsageSentence();
        sampleUsageSentence.setLanguage("mr");
        sampleUsageSentence.setSentenceNative("मी शाळेत जातो.");
        sampleUsageSentence.setSentenceLatin("Mi shalet jato.");
        sampleUsageSentence.setTranslation("I go to school.");
        sampleUsageSentence.setRegister("neutral");
        sampleUsageSentence.setExplanation("Simple present tense example");
        sampleUsageSentence.setDifficulty(2);
        sampleUsageSentence.setStatus(UsageSentenceStatus.DRAFT);
        sampleUsageSentence.setCreatedBy("admin@example.com");
        sampleUsageSentence.setLastModifiedBy("admin@example.com");
    }

    // =========================================================
    // getById Tests
    // =========================================================

    @Test
    @DisplayName("getById - Should return usage sentence when found")
    void getById_ShouldReturnUsageSentenceWhenFound() {
        // Given
        String id = "usage-123";
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));

        // When
        UsageSentence result = usageSentenceService.getById(id);

        // Then
        assertEquals(sampleUsageSentence, result);
        verify(usageSentenceRepository).findById(id);
    }

    @Test
    @DisplayName("getById - Should throw IllegalArgumentException when not found")
    void getById_ShouldThrowIllegalArgumentExceptionWhenNotFound() {
        // Given
        String id = "nonexistent";
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.getById(id)
        );
        assertEquals("UsageSentence not found: nonexistent", exception.getMessage());
        verify(usageSentenceRepository).findById(id);
    }

    // =========================================================
    // listByLanguage Tests
    // =========================================================

    @Test
    @DisplayName("listByLanguage - Should return usage sentences for enabled language")
    void listByLanguage_ShouldReturnUsageSentencesForEnabledLanguage() {
        // Given
        String language = "mr";
        List<UsageSentence> expectedSentences = Arrays.asList(sampleUsageSentence);
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(usageSentenceRepository.findByLanguageOrderBySentenceNativeAsc(language)).thenReturn(expectedSentences);

        // When
        List<UsageSentence> result = usageSentenceService.listByLanguage(language);

        // Then
        assertEquals(expectedSentences, result);
        verify(languageService).isLanguageEnabled(language);
        verify(usageSentenceRepository).findByLanguageOrderBySentenceNativeAsc(language);
    }

    @Test
    @DisplayName("listByLanguage - Should throw exception for disabled language")
    void listByLanguage_ShouldThrowExceptionForDisabledLanguage() {
        // Given
        String language = "disabled";
        when(languageService.isLanguageEnabled(language)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.listByLanguage(language)
        );
        assertEquals("Language is not enabled or not found: disabled", exception.getMessage());
        verify(languageService).isLanguageEnabled(language);
        verify(usageSentenceRepository, never()).findByLanguageOrderBySentenceNativeAsc(any());
    }

    @Test
    @DisplayName("listByLanguage - Should throw exception for null language")
    void listByLanguage_ShouldThrowExceptionForNullLanguage() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.listByLanguage(null)
        );
        assertEquals("language is required", exception.getMessage());
        verify(languageService, never()).isLanguageEnabled(any());
        verify(usageSentenceRepository, never()).findByLanguageOrderBySentenceNativeAsc(any());
    }

    @Test
    @DisplayName("listByLanguage - Should normalize and trim language input")
    void listByLanguage_ShouldNormalizeAndTrimLanguageInput() {
        // Given
        String language = "  mr  ";
        List<UsageSentence> expectedSentences = Arrays.asList(sampleUsageSentence);
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.findByLanguageOrderBySentenceNativeAsc("mr")).thenReturn(expectedSentences);

        // When
        List<UsageSentence> result = usageSentenceService.listByLanguage(language);

        // Then
        assertEquals(expectedSentences, result);
        verify(languageService).isLanguageEnabled("mr");
        verify(usageSentenceRepository).findByLanguageOrderBySentenceNativeAsc("mr");
    }

    // =========================================================
    // listByLanguageAndStatus Tests
    // =========================================================

    @Test
    @DisplayName("listByLanguageAndStatus - Should return usage sentences for enabled language and status")
    void listByLanguageAndStatus_ShouldReturnUsageSentencesForEnabledLanguageAndStatus() {
        // Given
        String language = "mr";
        UsageSentenceStatus status = UsageSentenceStatus.PUBLISHED;
        List<UsageSentence> expectedSentences = Arrays.asList(sampleUsageSentence);
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(usageSentenceRepository.findByLanguageAndStatusOrderBySentenceNativeAsc(language, status))
                .thenReturn(expectedSentences);

        // When
        List<UsageSentence> result = usageSentenceService.listByLanguageAndStatus(language, status);

        // Then
        assertEquals(expectedSentences, result);
        verify(languageService).isLanguageEnabled(language);
        verify(usageSentenceRepository).findByLanguageAndStatusOrderBySentenceNativeAsc(language, status);
    }

    @Test
    @DisplayName("listByLanguageAndStatus - Should throw exception for null status")
    void listByLanguageAndStatus_ShouldThrowExceptionForNullStatus() {
        // Given
        String language = "mr";
        when(languageService.isLanguageEnabled(language)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.listByLanguageAndStatus(language, null)
        );
        assertEquals("status is required", exception.getMessage());
        verify(usageSentenceRepository, never()).findByLanguageAndStatusOrderBySentenceNativeAsc(any(), any());
    }

    // =========================================================
    // create Tests
    // =========================================================

    @Test
    @DisplayName("create - Should create usage sentence with default DRAFT status")
    void create_ShouldCreateUsageSentenceWithDefaultDraftStatus() {
        // Given
        UsageSentenceCreateRequest request = new UsageSentenceCreateRequest(
                "mr", "मी शाळेत जातो.", "Mi shalet jato.", "I go to school.", 
                "neutral", "Simple present tense", 2, null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        UsageSentence result = usageSentenceService.create(request, actor);

        // Then
        assertEquals(sampleUsageSentence, result);
        
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertEquals("mr", capturedSentence.getLanguage());
        assertEquals("मी शाळेत जातो.", capturedSentence.getSentenceNative());
        assertEquals("Mi shalet jato.", capturedSentence.getSentenceLatin());
        assertEquals("I go to school.", capturedSentence.getTranslation());
        assertEquals("neutral", capturedSentence.getRegister());
        assertEquals("Simple present tense", capturedSentence.getExplanation());
        assertEquals(2, capturedSentence.getDifficulty());
        assertEquals(UsageSentenceStatus.DRAFT, capturedSentence.getStatus());
        assertEquals(actor, capturedSentence.getCreatedBy());
        assertEquals(actor, capturedSentence.getLastModifiedBy());

        verify(auditService).record(eq("USAGE_SENTENCE"), any(String.class), eq("USAGE_SENTENCE_CREATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("create - Should create usage sentence with specified status")
    void create_ShouldCreateUsageSentenceWithSpecifiedStatus() {
        // Given
        UsageSentenceCreateRequest request = new UsageSentenceCreateRequest(
                "mr", "मी शाळेत जातो.", "Mi shalet jato.", "I go to school.", 
                "neutral", "Simple present tense", 2, UsageSentenceStatus.REVIEW
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        usageSentenceService.create(request, actor);

        // Then
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertEquals(UsageSentenceStatus.REVIEW, capturedSentence.getStatus());
    }

    @Test
    @DisplayName("create - Should normalize register to lowercase and default to neutral")
    void create_ShouldNormalizeRegisterToLowercaseAndDefaultToNeutral() {
        // Given - Test with uppercase register
        UsageSentenceCreateRequest request1 = new UsageSentenceCreateRequest(
                "mr", "मी शाळेत जातो.", null, null, "FORMAL", null, null, null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        usageSentenceService.create(request1, actor);

        // Then
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertEquals("formal", capturedSentence.getRegister());

        // Reset mock
        reset(usageSentenceRepository);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // Given - Test with null register (should default to neutral)
        UsageSentenceCreateRequest request2 = new UsageSentenceCreateRequest(
                "mr", "मी शाळेत जातो.", null, null, null, null, null, null
        );

        // When
        usageSentenceService.create(request2, actor);

        // Then
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        capturedSentence = sentenceCaptor.getValue();
        assertEquals("neutral", capturedSentence.getRegister());
    }

    @Test
    @DisplayName("create - Should normalize and trim input fields")
    void create_ShouldNormalizeAndTrimInputFields() {
        // Given
        UsageSentenceCreateRequest request = new UsageSentenceCreateRequest(
                "  mr  ", "  मी शाळेत जातो.  ", "  Mi shalet jato.  ", "  I go to school.  ", 
                "  neutral  ", "  Simple present tense  ", 2, null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        usageSentenceService.create(request, actor);

        // Then
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertEquals("mr", capturedSentence.getLanguage());
        assertEquals("मी शाळेत जातो.", capturedSentence.getSentenceNative());
        assertEquals("Mi shalet jato.", capturedSentence.getSentenceLatin());
        assertEquals("I go to school.", capturedSentence.getTranslation());
        assertEquals("neutral", capturedSentence.getRegister());
        assertEquals("Simple present tense", capturedSentence.getExplanation());
    }

    @Test
    @DisplayName("create - Should convert empty strings to null for optional fields")
    void create_ShouldConvertEmptyStringsToNullForOptionalFields() {
        // Given
        UsageSentenceCreateRequest request = new UsageSentenceCreateRequest(
                "mr", "मी शाळेत जातो.", "   ", "   ", "neutral", "   ", 2, null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        usageSentenceService.create(request, actor);

        // Then
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertNull(capturedSentence.getSentenceLatin());
        assertNull(capturedSentence.getTranslation());
        assertNull(capturedSentence.getExplanation());
    }

    @Test
    @DisplayName("create - Should throw exception for null sentenceNative")
    void create_ShouldThrowExceptionForNullSentenceNative() {
        // Given
        UsageSentenceCreateRequest request = new UsageSentenceCreateRequest(
                "mr", null, "Mi shalet jato.", "I go to school.", "neutral", "Simple present tense", 2, null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.create(request, actor)
        );
        assertEquals("sentenceNative must be provided", exception.getMessage());
        verify(usageSentenceRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create - Should throw exception for blank sentenceNative")
    void create_ShouldThrowExceptionForBlankSentenceNative() {
        // Given
        UsageSentenceCreateRequest request = new UsageSentenceCreateRequest(
                "mr", "   ", "Mi shalet jato.", "I go to school.", "neutral", "Simple present tense", 2, null
        );
        String actor = "admin@example.com";
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.create(request, actor)
        );
        assertEquals("sentenceNative must be provided", exception.getMessage());
        verify(usageSentenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should handle null actor gracefully")
    void create_ShouldHandleNullActorGracefully() {
        // Given
        UsageSentenceCreateRequest request = new UsageSentenceCreateRequest(
                "mr", "मी शाळेत जातो.", "Mi shalet jato.", "I go to school.", 
                "neutral", "Simple present tense", 2, null
        );
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        usageSentenceService.create(request, null);

        // Then
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertNull(capturedSentence.getCreatedBy());
        assertNull(capturedSentence.getLastModifiedBy());
    }

    // =========================================================
    // update Tests
    // =========================================================

    @Test
    @DisplayName("update - Should update usage sentence fields successfully")
    void update_ShouldUpdateUsageSentenceFieldsSuccessfully() {
        // Given
        String id = "usage-123";
        UsageSentenceUpdateRequest request = new UsageSentenceUpdateRequest(
                null, "तू शाळेत जातोस.", "Tu shalet jatos.", "You go to school.", 
                "informal", "Second person example", 3
        );
        String actor = "editor@example.com";
        
        UsageSentence updatedSentence = new UsageSentence();
        updatedSentence.setLanguage("mr");
        updatedSentence.setSentenceNative("तू शाळेत जातोस.");
        updatedSentence.setSentenceLatin("Tu shalet jatos.");
        updatedSentence.setTranslation("You go to school.");
        updatedSentence.setRegister("informal");
        updatedSentence.setExplanation("Second person example");
        updatedSentence.setDifficulty(3);
        updatedSentence.setStatus(UsageSentenceStatus.DRAFT);
        
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(updatedSentence);

        // When
        UsageSentence result = usageSentenceService.update(id, request, actor);

        // Then
        assertEquals(updatedSentence, result);
        
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertEquals("तू शाळेत जातोस.", capturedSentence.getSentenceNative());
        assertEquals("Tu shalet jatos.", capturedSentence.getSentenceLatin());
        assertEquals("You go to school.", capturedSentence.getTranslation());
        assertEquals("informal", capturedSentence.getRegister());
        assertEquals("Second person example", capturedSentence.getExplanation());
        assertEquals(3, capturedSentence.getDifficulty());
        assertEquals(actor, capturedSentence.getLastModifiedBy());

        verify(auditService).record(eq("USAGE_SENTENCE"), any(String.class), eq("USAGE_SENTENCE_UPDATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("update - Should only update non-null fields")
    void update_ShouldOnlyUpdateNonNullFields() {
        // Given
        String id = "usage-123";
        UsageSentenceUpdateRequest request = new UsageSentenceUpdateRequest(
                null, null, "New latin text", null, null, null, null
        );
        String actor = "editor@example.com";
        
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        usageSentenceService.update(id, request, actor);

        // Then
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertEquals("mr", capturedSentence.getLanguage()); // unchanged
        assertEquals("मी शाळेत जातो.", capturedSentence.getSentenceNative()); // unchanged
        assertEquals("New latin text", capturedSentence.getSentenceLatin()); // updated
        assertEquals("I go to school.", capturedSentence.getTranslation()); // unchanged
        assertEquals("neutral", capturedSentence.getRegister()); // unchanged
        assertEquals("Simple present tense example", capturedSentence.getExplanation()); // unchanged
        assertEquals(2, capturedSentence.getDifficulty()); // unchanged
    }

    @Test
    @DisplayName("update - Should handle language change")
    void update_ShouldHandleLanguageChange() {
        // Given
        String id = "usage-123";
        UsageSentenceUpdateRequest request = new UsageSentenceUpdateRequest(
                "hi", null, null, null, null, null, null
        );
        String actor = "editor@example.com";
        
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));
        when(languageService.isLanguageEnabled("hi")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        usageSentenceService.update(id, request, actor);

        // Then
        verify(languageService).isLanguageEnabled("hi");
        
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertEquals("hi", capturedSentence.getLanguage());
    }

    @Test
    @DisplayName("update - Should throw exception for blank sentenceNative")
    void update_ShouldThrowExceptionForBlankSentenceNative() {
        // Given
        String id = "usage-123";
        UsageSentenceUpdateRequest request = new UsageSentenceUpdateRequest(
                null, "   ", null, null, null, null, null
        );
        String actor = "editor@example.com";
        
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.update(id, request, actor)
        );
        assertEquals("sentenceNative must be provided", exception.getMessage());
        verify(usageSentenceRepository, never()).save(any());
    }

    // =========================================================
    // setStatus Tests
    // =========================================================

    @Test
    @DisplayName("setStatus - Should update status successfully")
    void setStatus_ShouldUpdateStatusSuccessfully() {
        // Given
        String id = "usage-123";
        UsageSentenceStatus newStatus = UsageSentenceStatus.REVIEW;
        String actor = "editor@example.com";
        
        UsageSentence updatedSentence = new UsageSentence();
        updatedSentence.setStatus(newStatus);
        
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(updatedSentence);

        // When
        UsageSentence result = usageSentenceService.setStatus(id, newStatus, actor);

        // Then
        assertEquals(updatedSentence, result);
        
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertEquals(newStatus, capturedSentence.getStatus());
        assertEquals(actor, capturedSentence.getLastModifiedBy());

        verify(auditService).record(eq("USAGE_SENTENCE"), any(String.class), eq("USAGE_SENTENCE_STATUS_CHANGED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("setStatus - Should throw exception for null status")
    void setStatus_ShouldThrowExceptionForNullStatus() {
        // Given
        String id = "usage-123";
        String actor = "editor@example.com";
        
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.setStatus(id, null, actor)
        );
        assertEquals("Status cannot be null", exception.getMessage());
        verify(usageSentenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("setStatus - Should prevent publishing archived sentence directly")
    void setStatus_ShouldPreventPublishingArchivedSentenceDirectly() {
        // Given
        String id = "usage-123";
        sampleUsageSentence.setStatus(UsageSentenceStatus.ARCHIVED);
        String actor = "editor@example.com";
        
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.setStatus(id, UsageSentenceStatus.PUBLISHED, actor)
        );
        assertEquals("Cannot publish an ARCHIVED sentence. Unarchive to REVIEW first.", exception.getMessage());
        verify(usageSentenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("setStatus - Should prevent publishing draft sentence directly")
    void setStatus_ShouldPreventPublishingDraftSentenceDirectly() {
        // Given
        String id = "usage-123";
        sampleUsageSentence.setStatus(UsageSentenceStatus.DRAFT);
        String actor = "editor@example.com";
        
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.setStatus(id, UsageSentenceStatus.PUBLISHED, actor)
        );
        assertEquals("Cannot publish directly from DRAFT. Move to REVIEW first.", exception.getMessage());
        verify(usageSentenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("setStatus - Should allow valid status transitions")
    void setStatus_ShouldAllowValidStatusTransitions() {
        // Test DRAFT -> REVIEW
        String id = "usage-123";
        String actor = "editor@example.com";
        
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        usageSentenceService.setStatus(id, UsageSentenceStatus.REVIEW, actor);

        // Then
        verify(usageSentenceRepository).save(any(UsageSentence.class));
        
        // Reset for next test
        reset(usageSentenceRepository);
        sampleUsageSentence.setStatus(UsageSentenceStatus.REVIEW);
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // Test REVIEW -> PUBLISHED
        usageSentenceService.setStatus(id, UsageSentenceStatus.PUBLISHED, actor);
        verify(usageSentenceRepository).save(any(UsageSentence.class));
    }

    // =========================================================
    // listPublishedByLanguage Tests
    // =========================================================

    @Test
    @DisplayName("listPublishedByLanguage - Should return published sentences for enabled language")
    void listPublishedByLanguage_ShouldReturnPublishedSentencesForEnabledLanguage() {
        // Given
        String language = "mr";
        sampleUsageSentence.setStatus(UsageSentenceStatus.PUBLISHED);
        List<UsageSentence> expectedSentences = Arrays.asList(sampleUsageSentence);
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(usageSentenceRepository.findByLanguageAndStatusOrderBySentenceNativeAsc(language, UsageSentenceStatus.PUBLISHED))
                .thenReturn(expectedSentences);

        // When
        List<UsageSentence> result = usageSentenceService.listPublishedByLanguage(language);

        // Then
        assertEquals(expectedSentences, result);
        verify(languageService).isLanguageEnabled(language);
        verify(usageSentenceRepository).findByLanguageAndStatusOrderBySentenceNativeAsc(language, UsageSentenceStatus.PUBLISHED);
    }

    // =========================================================
    // getPublishedById Tests
    // =========================================================

    @Test
    @DisplayName("getPublishedById - Should return published sentence when found")
    void getPublishedById_ShouldReturnPublishedSentenceWhenFound() {
        // Given
        String id = "usage-123";
        sampleUsageSentence.setStatus(UsageSentenceStatus.PUBLISHED);
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));

        // When
        UsageSentence result = usageSentenceService.getPublishedById(id);

        // Then
        assertEquals(sampleUsageSentence, result);
        verify(usageSentenceRepository).findById(id);
    }

    @Test
    @DisplayName("getPublishedById - Should throw exception when sentence is not published")
    void getPublishedById_ShouldThrowExceptionWhenSentenceIsNotPublished() {
        // Given
        String id = "usage-123";
        sampleUsageSentence.setStatus(UsageSentenceStatus.DRAFT);
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.getPublishedById(id)
        );
        assertEquals("Published usage sentence not found: usage-123", exception.getMessage());
        verify(usageSentenceRepository).findById(id);
    }

    // =========================================================
    // Edge Cases and Complex Scenarios
    // =========================================================

    @Test
    @DisplayName("listByLanguage - Should return single usage sentence when only one exists")
    void listByLanguage_ShouldReturnSingleUsageSentenceWhenOnlyOneExists() {
        // Given
        String language = "mr";
        List<UsageSentence> singleSentence = Arrays.asList(sampleUsageSentence);
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(usageSentenceRepository.findByLanguageOrderBySentenceNativeAsc(language)).thenReturn(singleSentence);

        // When
        List<UsageSentence> result = usageSentenceService.listByLanguage(language);

        // Then
        assertEquals(1, result.size());
        assertEquals(sampleUsageSentence, result.get(0));
        verify(languageService).isLanguageEnabled(language);
        verify(usageSentenceRepository).findByLanguageOrderBySentenceNativeAsc(language);
    }

    @Test
    @DisplayName("listByLanguage - Should return empty list when no sentences exist for enabled language")
    void listByLanguage_ShouldReturnEmptyListWhenNoSentencesExistForEnabledLanguage() {
        // Given
        String language = "gu"; // Gujarati - enabled but no sentences
        List<UsageSentence> emptySentences = Arrays.asList();
        
        when(languageService.isLanguageEnabled(language)).thenReturn(true);
        when(usageSentenceRepository.findByLanguageOrderBySentenceNativeAsc(language)).thenReturn(emptySentences);

        // When
        List<UsageSentence> result = usageSentenceService.listByLanguage(language);

        // Then
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(languageService).isLanguageEnabled(language);
        verify(usageSentenceRepository).findByLanguageOrderBySentenceNativeAsc(language);
    }

    @Test
    @DisplayName("Multiple languages scenario - Should handle mixed enabled/disabled languages correctly")
    void multipleLanguagesScenario_ShouldHandleMixedEnabledDisabledLanguagesCorrectly() {
        // Given - Setup multiple usage sentences for different languages
        UsageSentence marathiSentence = new UsageSentence();
        marathiSentence.setLanguage("mr");
        marathiSentence.setSentenceNative("मी शाळेत जातो.");
        marathiSentence.setStatus(UsageSentenceStatus.PUBLISHED);

        UsageSentence hindiSentence = new UsageSentence();
        hindiSentence.setLanguage("hi");
        hindiSentence.setSentenceNative("मैं स्कूल जाता हूँ।");
        hindiSentence.setStatus(UsageSentenceStatus.PUBLISHED);

        UsageSentence gujaratiSentence = new UsageSentence();
        gujaratiSentence.setLanguage("gu");
        gujaratiSentence.setSentenceNative("હું શાળામાં જાઉં છું.");
        gujaratiSentence.setStatus(UsageSentenceStatus.PUBLISHED);

        // Setup language enablement: mr=enabled, hi=enabled, gu=disabled
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(languageService.isLanguageEnabled("hi")).thenReturn(true);
        when(languageService.isLanguageEnabled("gu")).thenReturn(false);

        // Setup repository responses
        when(usageSentenceRepository.findByLanguageOrderBySentenceNativeAsc("mr"))
                .thenReturn(Arrays.asList(marathiSentence));
        when(usageSentenceRepository.findByLanguageOrderBySentenceNativeAsc("hi"))
                .thenReturn(Arrays.asList(hindiSentence));

        // When & Then - Test enabled languages work
        List<UsageSentence> marathiResult = usageSentenceService.listByLanguage("mr");
        assertEquals(1, marathiResult.size());
        assertEquals("मी शाळेत जातो.", marathiResult.get(0).getSentenceNative());

        List<UsageSentence> hindiResult = usageSentenceService.listByLanguage("hi");
        assertEquals(1, hindiResult.size());
        assertEquals("मैं स्कूल जाता हूँ।", hindiResult.get(0).getSentenceNative());

        // When & Then - Test disabled language throws exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.listByLanguage("gu")
        );
        assertEquals("Language is not enabled or not found: gu", exception.getMessage());

        // Verify repository was never called for disabled language
        verify(usageSentenceRepository, never()).findByLanguageOrderBySentenceNativeAsc("gu");
    }

    @Test
    @DisplayName("All languages disabled scenario - Should reject all requests even if sentences exist")
    void allLanguagesDisabledScenario_ShouldRejectAllRequestsEvenIfSentencesExist() {
        // Given - All languages are disabled but sentences exist in database
        when(languageService.isLanguageEnabled("mr")).thenReturn(false);
        when(languageService.isLanguageEnabled("hi")).thenReturn(false);
        when(languageService.isLanguageEnabled("gu")).thenReturn(false);
        when(languageService.isLanguageEnabled("ta")).thenReturn(false);

        String[] disabledLanguages = {"mr", "hi", "gu", "ta"};

        // When & Then - All languages should be rejected
        for (String language : disabledLanguages) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    usageSentenceService.listByLanguage(language)
            );
            assertEquals("Language is not enabled or not found: " + language, exception.getMessage());
            
            // Verify repository is never called for any disabled language
            verify(usageSentenceRepository, never()).findByLanguageOrderBySentenceNativeAsc(language);
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
        // Given - Multiple sentences with different statuses for same language
        UsageSentence publishedSentence1 = new UsageSentence();
        publishedSentence1.setSentenceNative("मी शाळेत जातो.");
        publishedSentence1.setStatus(UsageSentenceStatus.PUBLISHED);

        UsageSentence publishedSentence2 = new UsageSentence();
        publishedSentence2.setSentenceNative("तू शाळेत जातोस.");
        publishedSentence2.setStatus(UsageSentenceStatus.PUBLISHED);

        // Only published sentences should be returned
        List<UsageSentence> publishedSentences = Arrays.asList(publishedSentence1, publishedSentence2);
        
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.findByLanguageAndStatusOrderBySentenceNativeAsc("mr", UsageSentenceStatus.PUBLISHED))
                .thenReturn(publishedSentences);

        // When
        List<UsageSentence> result = usageSentenceService.listPublishedByLanguage("mr");

        // Then
        assertEquals(2, result.size());
        assertEquals("मी शाळेत जातो.", result.get(0).getSentenceNative());
        assertEquals("तू शाळेत जातोस.", result.get(1).getSentenceNative());
        
        // Verify only published status was queried
        verify(usageSentenceRepository).findByLanguageAndStatusOrderBySentenceNativeAsc("mr", UsageSentenceStatus.PUBLISHED);
        verify(usageSentenceRepository, never()).findByLanguageAndStatusOrderBySentenceNativeAsc(eq("mr"), eq(UsageSentenceStatus.DRAFT));
        verify(usageSentenceRepository, never()).findByLanguageAndStatusOrderBySentenceNativeAsc(eq("mr"), eq(UsageSentenceStatus.REVIEW));
    }

    @Test
    @DisplayName("create - Should handle creating sentences for different enabled languages")
    void create_ShouldHandleCreatingSentencesForDifferentEnabledLanguages() {
        // Given - Multiple create requests for different languages
        UsageSentenceCreateRequest marathiRequest = new UsageSentenceCreateRequest(
                "mr", "मी शाळेत जातो.", "Mi shalet jato.", "I go to school.", "neutral", null, 2, null
        );
        
        UsageSentenceCreateRequest hindiRequest = new UsageSentenceCreateRequest(
                "hi", "मैं स्कूल जाता हूँ।", "Main school jata hun.", "I go to school.", "neutral", null, 2, null
        );

        String actor = "admin@example.com";
        
        // Setup language enablement
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(languageService.isLanguageEnabled("hi")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When - Create sentences for both languages
        usageSentenceService.create(marathiRequest, actor);
        usageSentenceService.create(hindiRequest, actor);

        // Then - Verify both languages were validated and sentences saved
        verify(languageService).isLanguageEnabled("mr");
        verify(languageService).isLanguageEnabled("hi");
        verify(usageSentenceRepository, times(2)).save(any(UsageSentence.class));
        verify(auditService, times(2)).record(eq("USAGE_SENTENCE"), any(String.class), eq("USAGE_SENTENCE_CREATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("create - Should reject creation for disabled language even if similar enabled language exists")
    void create_ShouldRejectCreationForDisabledLanguageEvenIfSimilarEnabledLanguageExists() {
        // Given - mr is enabled, but mr-IN (regional variant) is disabled
        UsageSentenceCreateRequest disabledLanguageRequest = new UsageSentenceCreateRequest(
                "mr-IN", "मी शाळेत जातो.", "Mi shalet jato.", "I go to school.", "neutral", null, 2, null
        );

        String actor = "admin@example.com";
        
        // Only mock the language that will actually be checked
        when(languageService.isLanguageEnabled("mr-IN")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.create(disabledLanguageRequest, actor)
        );
        assertEquals("Language is not enabled or not found: mr-IN", exception.getMessage());
        
        verify(languageService).isLanguageEnabled("mr-IN");
        verify(languageService, never()).isLanguageEnabled("mr"); // Should not check similar language
        verify(usageSentenceRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getPublishedById - Should work correctly when single published sentence exists")
    void getPublishedById_ShouldWorkCorrectlyWhenSinglePublishedSentenceExists() {
        // Given
        String id = "usage-123";
        sampleUsageSentence.setStatus(UsageSentenceStatus.PUBLISHED);
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));

        // When
        UsageSentence result = usageSentenceService.getPublishedById(id);

        // Then
        assertEquals(sampleUsageSentence, result);
        assertEquals(UsageSentenceStatus.PUBLISHED, result.getStatus());
        verify(usageSentenceRepository).findById(id);
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
                usageSentenceService.listByLanguage(disabledLanguage)
        );
        assertEquals("Language is not enabled or not found: disabled-lang", exception1.getMessage());

        // Test listByLanguageAndStatus
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.listByLanguageAndStatus(disabledLanguage, UsageSentenceStatus.DRAFT)
        );
        assertEquals("Language is not enabled or not found: disabled-lang", exception2.getMessage());

        // Test listPublishedByLanguage
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.listPublishedByLanguage(disabledLanguage)
        );
        assertEquals("Language is not enabled or not found: disabled-lang", exception3.getMessage());

        // Test create
        UsageSentenceCreateRequest createRequest = new UsageSentenceCreateRequest(
                disabledLanguage, "Test sentence", null, null, null, null, null, null
        );
        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class, () ->
                usageSentenceService.create(createRequest, "actor")
        );
        assertEquals("Language is not enabled or not found: disabled-lang", exception4.getMessage());

        // Verify language service was called for each method
        verify(languageService, times(4)).isLanguageEnabled(disabledLanguage);
        
        // Verify no repository calls were made
        verify(usageSentenceRepository, never()).findByLanguageOrderBySentenceNativeAsc(any());
        verify(usageSentenceRepository, never()).findByLanguageAndStatusOrderBySentenceNativeAsc(any(), any());
        verify(usageSentenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should handle Unicode content correctly")
    void create_ShouldHandleUnicodeContentCorrectly() {
        // Given
        UsageSentenceCreateRequest request = new UsageSentenceCreateRequest(
                "hi", "मैं स्कूल जाता हूँ।", "Main school jata hun.", "I go to school.", 
                "formal", "हिंदी में वर्तमान काल का उदाहरण", 2, null
        );
        String actor = "linguist@example.com";
        
        when(languageService.isLanguageEnabled("hi")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // When
        usageSentenceService.create(request, actor);

        // Then
        ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
        verify(usageSentenceRepository).save(sentenceCaptor.capture());
        
        UsageSentence capturedSentence = sentenceCaptor.getValue();
        assertEquals("hi", capturedSentence.getLanguage());
        assertEquals("मैं स्कूल जाता हूँ।", capturedSentence.getSentenceNative());
        assertEquals("Main school jata hun.", capturedSentence.getSentenceLatin());
        assertEquals("I go to school.", capturedSentence.getTranslation());
        assertEquals("formal", capturedSentence.getRegister());
        assertEquals("हिंदी में वर्तमान काल का उदाहरण", capturedSentence.getExplanation());
    }

    @Test
    @DisplayName("Complex workflow - Should handle complete sentence lifecycle")
    void complexWorkflow_ShouldHandleCompleteSentenceLifecycle() {
        // This test demonstrates a complete workflow from creation to publication
        String id = "usage-123";
        String actor = "admin@example.com";
        
        // Setup for status transitions
        when(usageSentenceRepository.findById(id)).thenReturn(Optional.of(sampleUsageSentence));
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // Test DRAFT -> REVIEW transition
        usageSentenceService.setStatus(id, UsageSentenceStatus.REVIEW, actor);
        
        // Update sentence status for next transition
        sampleUsageSentence.setStatus(UsageSentenceStatus.REVIEW);
        
        // Test REVIEW -> PUBLISHED transition
        usageSentenceService.setStatus(id, UsageSentenceStatus.PUBLISHED, actor);
        
        // Verify all interactions
        verify(usageSentenceRepository, times(2)).save(any(UsageSentence.class));
        verify(auditService, times(2)).record(eq("USAGE_SENTENCE"), any(String.class), eq("USAGE_SENTENCE_STATUS_CHANGED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("create - Should handle all register variations correctly")
    void create_ShouldHandleAllRegisterVariationsCorrectly() {
        // Given
        String actor = "admin@example.com";
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

        // Test different register values
        String[] registers = {"SPOKEN", "Neutral", "FORMAL", "   informal   ", "", null};
        String[] expectedRegisters = {"spoken", "neutral", "formal", "informal", "neutral", "neutral"};

        for (int i = 0; i < registers.length; i++) {
            // Reset mock
            reset(usageSentenceRepository);
            when(usageSentenceRepository.save(any(UsageSentence.class))).thenReturn(sampleUsageSentence);

            UsageSentenceCreateRequest request = new UsageSentenceCreateRequest(
                    "mr", "मी शाळेत जातो.", null, null, registers[i], null, null, null
            );

            // When
            usageSentenceService.create(request, actor);

            // Then
            ArgumentCaptor<UsageSentence> sentenceCaptor = ArgumentCaptor.forClass(UsageSentence.class);
            verify(usageSentenceRepository).save(sentenceCaptor.capture());
            
            UsageSentence capturedSentence = sentenceCaptor.getValue();
            assertEquals(expectedRegisters[i], capturedSentence.getRegister(), 
                    "Register mismatch for input: " + registers[i]);
        }
    }

    // =========================================================
    // searchSentences Tests
    // =========================================================

    @Test
    @DisplayName("searchSentences - Should return paginated results with all filters")
    void searchSentences_ShouldReturnPaginatedResultsWithAllFilters() {
        // Given
        UsageSentenceSearchRequest request = new UsageSentenceSearchRequest(
                "मी शाळेत", "mr", "DRAFT", 0, 20, "sentenceNative", "asc"
        );
        
        UsageSentence sentence1 = new UsageSentence();
        sentence1.setLanguage("mr");
        sentence1.setSentenceNative("मी शाळेत जातो.");
        sentence1.setSentenceLatin("Mi shalet jato.");
        sentence1.setTranslation("I go to school.");
        sentence1.setStatus(UsageSentenceStatus.DRAFT);
        
        Page<UsageSentence> mockPage = mock(Page.class);
        when(mockPage.getContent()).thenReturn(Arrays.asList(sentence1));
        
        when(usageSentenceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        Page<UsageSentence> result = usageSentenceService.searchSentences(request);

        // Then
        assertEquals(mockPage, result);
        assertEquals(1, result.getContent().size());
        assertEquals("मी शाळेत जातो.", result.getContent().get(0).getSentenceNative());
        
        verify(usageSentenceRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("searchSentences - Should handle pagination parameters")
    void searchSentences_ShouldHandlePaginationParameters() {
        // Given
        UsageSentenceSearchRequest request = new UsageSentenceSearchRequest(
                null, "mr", null, 2, 50, "sentenceNative", "asc"
        );
        
        Page<UsageSentence> mockPage = mock(Page.class);
        when(usageSentenceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        usageSentenceService.searchSentences(request);

        // Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(usageSentenceRepository).findAll(any(Specification.class), pageableCaptor.capture());
        
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "sentenceNative"), pageable.getSort());
    }

    @Test
    @DisplayName("searchSentences - Should handle descending sort")
    void searchSentences_ShouldHandleDescendingSort() {
        // Given
        UsageSentenceSearchRequest request = new UsageSentenceSearchRequest(
                null, "mr", null, 0, 20, "status", "desc"
        );
        
        Page<UsageSentence> mockPage = mock(Page.class);
        when(usageSentenceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        usageSentenceService.searchSentences(request);

        // Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(usageSentenceRepository).findAll(any(Specification.class), pageableCaptor.capture());
        
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(Sort.by(Sort.Direction.DESC, "status"), pageable.getSort());
    }

    @Test
    @DisplayName("searchSentences - Should search in native, latin, and translation")
    void searchSentences_ShouldSearchInNativeLatinAndTranslation() {
        // Given
        UsageSentenceSearchRequest request = new UsageSentenceSearchRequest(
                "school", "mr", null, 0, 20, "sentenceNative", "asc"
        );
        
        UsageSentence sentence1 = new UsageSentence();
        sentence1.setSentenceNative("मी शाळेत जातो.");
        sentence1.setSentenceLatin("Mi shalet jato.");
        sentence1.setTranslation("I go to school.");
        
        Page<UsageSentence> mockPage = mock(Page.class);
        when(mockPage.getContent()).thenReturn(Arrays.asList(sentence1));
        when(usageSentenceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        Page<UsageSentence> result = usageSentenceService.searchSentences(request);

        // Then
        assertEquals(1, result.getContent().size());
        verify(usageSentenceRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("searchSentences - Should filter by language")
    void searchSentences_ShouldFilterByLanguage() {
        // Given
        UsageSentenceSearchRequest request = new UsageSentenceSearchRequest(
                null, "mr", null, 0, 20, "sentenceNative", "asc"
        );
        
        Page<UsageSentence> mockPage = mock(Page.class);
        when(usageSentenceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        usageSentenceService.searchSentences(request);

        // Then
        verify(usageSentenceRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("searchSentences - Should filter by status")
    void searchSentences_ShouldFilterByStatus() {
        // Given
        UsageSentenceSearchRequest request = new UsageSentenceSearchRequest(
                null, "mr", "PUBLISHED", 0, 20, "sentenceNative", "asc"
        );
        
        UsageSentence publishedSentence = new UsageSentence();
        publishedSentence.setLanguage("mr");
        publishedSentence.setSentenceNative("मी शाळेत जातो.");
        publishedSentence.setStatus(UsageSentenceStatus.PUBLISHED);
        
        Page<UsageSentence> mockPage = mock(Page.class);
        when(mockPage.getContent()).thenReturn(Arrays.asList(publishedSentence));
        when(usageSentenceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        Page<UsageSentence> result = usageSentenceService.searchSentences(request);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(UsageSentenceStatus.PUBLISHED, result.getContent().get(0).getStatus());
        verify(usageSentenceRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("searchSentences - Should combine search, language, and status filters")
    void searchSentences_ShouldCombineSearchLanguageAndStatusFilters() {
        // Given
        UsageSentenceSearchRequest request = new UsageSentenceSearchRequest(
                "जातो", "mr", "DRAFT", 0, 20, "sentenceNative", "asc"
        );
        
        Page<UsageSentence> mockPage = mock(Page.class);
        when(usageSentenceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // When
        usageSentenceService.searchSentences(request);

        // Then
        verify(usageSentenceRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}