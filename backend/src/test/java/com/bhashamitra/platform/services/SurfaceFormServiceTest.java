package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.LemmaStatus;
import com.bhashamitra.platform.models.SurfaceForm;
import com.bhashamitra.platform.repositories.LemmaRepository;
import com.bhashamitra.platform.repositories.SurfaceFormRepository;
import com.bhashamitra.platform.services.SurfaceFormService.SurfaceFormCreateRequest;
import com.bhashamitra.platform.services.SurfaceFormService.SurfaceFormUpdateRequest;
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
@DisplayName("SurfaceFormService Tests")
class SurfaceFormServiceTest {

    @Mock
    private SurfaceFormRepository surfaceFormRepository;

    @Mock
    private LemmaRepository lemmaRepository;

    @Mock
    private LanguageService languageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SurfaceFormService surfaceFormService;

    private SurfaceForm sampleSurfaceForm;
    private Lemma sampleLemma;

    @BeforeEach
    void setUp() {
        // Setup sample lemma
        sampleLemma = new Lemma();
        sampleLemma.setLanguage("mr");
        sampleLemma.setLemmaNative("नमस्कार");
        sampleLemma.setLemmaLatin("namaskar");
        sampleLemma.setPos("noun");
        sampleLemma.setStatus(LemmaStatus.PUBLISHED);

        // Setup sample surface form
        sampleSurfaceForm = new SurfaceForm();
        sampleSurfaceForm.setLemma(sampleLemma);
        sampleSurfaceForm.setFormNative("नमस्कारा");
        sampleSurfaceForm.setFormLatin("namaskara");
        sampleSurfaceForm.setFormType("oblique");
        sampleSurfaceForm.setNotes("Oblique case form");
        sampleSurfaceForm.setCreatedBy("admin@example.com");
        sampleSurfaceForm.setLastModifiedBy("admin@example.com");
    }

    // =========================================================
    // getById Tests
    // =========================================================

    @Test
    @DisplayName("getById - Should return surface form when found")
    void getById_ShouldReturnSurfaceFormWhenFound() {
        // Given
        String id = "surface-123";
        when(surfaceFormRepository.findById(id)).thenReturn(Optional.of(sampleSurfaceForm));

        // When
        SurfaceForm result = surfaceFormService.getById(id);

        // Then
        assertEquals(sampleSurfaceForm, result);
        verify(surfaceFormRepository).findById(id);
    }

    @Test
    @DisplayName("getById - Should throw IllegalArgumentException when not found")
    void getById_ShouldThrowIllegalArgumentExceptionWhenNotFound() {
        // Given
        String id = "nonexistent";
        when(surfaceFormRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.getById(id)
        );
        assertEquals("SurfaceForm not found: nonexistent", exception.getMessage());
        verify(surfaceFormRepository).findById(id);
    }

    // =========================================================
    // listByLemmaId Tests
    // =========================================================

    @Test
    @DisplayName("listByLemmaId - Should return surface forms for valid lemma ID")
    void listByLemmaId_ShouldReturnSurfaceFormsForValidLemmaId() {
        // Given
        String lemmaId = "lemma-123";
        List<SurfaceForm> expectedForms = Arrays.asList(sampleSurfaceForm);
        
        when(surfaceFormRepository.findByLemma_IdOrderByFormNativeAscIdAsc(lemmaId)).thenReturn(expectedForms);

        // When
        List<SurfaceForm> result = surfaceFormService.listByLemmaId(lemmaId);

        // Then
        assertEquals(expectedForms, result);
        verify(surfaceFormRepository).findByLemma_IdOrderByFormNativeAscIdAsc(lemmaId);
    }

    @Test
    @DisplayName("listByLemmaId - Should throw exception for null lemma ID")
    void listByLemmaId_ShouldThrowExceptionForNullLemmaId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.listByLemmaId(null)
        );
        assertEquals("lemmaId must be provided", exception.getMessage());
        verify(surfaceFormRepository, never()).findByLemma_IdOrderByFormNativeAscIdAsc(any());
    }

    @Test
    @DisplayName("listByLemmaId - Should throw exception for blank lemma ID")
    void listByLemmaId_ShouldThrowExceptionForBlankLemmaId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.listByLemmaId("   ")
        );
        assertEquals("lemmaId must be provided", exception.getMessage());
        verify(surfaceFormRepository, never()).findByLemma_IdOrderByFormNativeAscIdAsc(any());
    }

    @Test
    @DisplayName("listByLemmaId - Should return empty list when no forms exist")
    void listByLemmaId_ShouldReturnEmptyListWhenNoFormsExist() {
        // Given
        String lemmaId = "lemma-no-forms";
        List<SurfaceForm> emptyForms = Arrays.asList();
        
        when(surfaceFormRepository.findByLemma_IdOrderByFormNativeAscIdAsc(lemmaId)).thenReturn(emptyForms);

        // When
        List<SurfaceForm> result = surfaceFormService.listByLemmaId(lemmaId);

        // Then
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(surfaceFormRepository).findByLemma_IdOrderByFormNativeAscIdAsc(lemmaId);
    }

    // =========================================================
    // create Tests
    // =========================================================

    @Test
    @DisplayName("create - Should create surface form successfully")
    void create_ShouldCreateSurfaceFormSuccessfully() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "lemma-123", "नमस्कारा", "namaskara", "oblique", "Oblique case form"
        );
        String actor = "admin@example.com";
        
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(surfaceFormRepository.existsByLemma_IdAndFormNative("lemma-123", "नमस्कारा")).thenReturn(false);
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

        // When
        SurfaceForm result = surfaceFormService.create(request, actor);

        // Then
        assertEquals(sampleSurfaceForm, result);
        
        ArgumentCaptor<SurfaceForm> formCaptor = ArgumentCaptor.forClass(SurfaceForm.class);
        verify(surfaceFormRepository).save(formCaptor.capture());
        
        SurfaceForm capturedForm = formCaptor.getValue();
        assertEquals(sampleLemma, capturedForm.getLemma());
        assertEquals("नमस्कारा", capturedForm.getFormNative());
        assertEquals("namaskara", capturedForm.getFormLatin());
        assertEquals("oblique", capturedForm.getFormType());
        assertEquals("Oblique case form", capturedForm.getNotes());
        assertEquals(actor, capturedForm.getCreatedBy());
        assertEquals(actor, capturedForm.getLastModifiedBy());

        verify(auditService).record(eq("SURFACE_FORM"), any(String.class), eq("SURFACE_FORM_CREATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("create - Should normalize and trim input fields")
    void create_ShouldNormalizeAndTrimInputFields() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "  lemma-123  ", "  नमस्कारा  ", "  namaskara  ", "  oblique  ", "Oblique case form"
        );
        String actor = "admin@example.com";
        
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(surfaceFormRepository.existsByLemma_IdAndFormNative("lemma-123", "नमस्कारा")).thenReturn(false);
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

        // When
        surfaceFormService.create(request, actor);

        // Then
        verify(lemmaRepository).findById("lemma-123");
        verify(surfaceFormRepository).existsByLemma_IdAndFormNative("lemma-123", "नमस्कारा");
        
        ArgumentCaptor<SurfaceForm> formCaptor = ArgumentCaptor.forClass(SurfaceForm.class);
        verify(surfaceFormRepository).save(formCaptor.capture());
        
        SurfaceForm capturedForm = formCaptor.getValue();
        assertEquals("नमस्कारा", capturedForm.getFormNative());
        assertEquals("namaskara", capturedForm.getFormLatin());
        assertEquals("oblique", capturedForm.getFormType());
    }

    @Test
    @DisplayName("create - Should convert empty strings to null for optional fields")
    void create_ShouldConvertEmptyStringsToNullForOptionalFields() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "lemma-123", "नमस्कारा", "   ", "   ", "Notes"
        );
        String actor = "admin@example.com";
        
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(surfaceFormRepository.existsByLemma_IdAndFormNative("lemma-123", "नमस्कारा")).thenReturn(false);
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

        // When
        surfaceFormService.create(request, actor);

        // Then
        ArgumentCaptor<SurfaceForm> formCaptor = ArgumentCaptor.forClass(SurfaceForm.class);
        verify(surfaceFormRepository).save(formCaptor.capture());
        
        SurfaceForm capturedForm = formCaptor.getValue();
        assertNull(capturedForm.getFormLatin());
        assertNull(capturedForm.getFormType());
        assertEquals("Notes", capturedForm.getNotes()); // notes kept as-is
    }

    @Test
    @DisplayName("create - Should throw exception for null lemma ID")
    void create_ShouldThrowExceptionForNullLemmaId() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                null, "नमस्कारा", "namaskara", "oblique", "Notes"
        );
        String actor = "admin@example.com";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.create(request, actor)
        );
        assertEquals("lemmaId must be provided", exception.getMessage());
        verify(lemmaRepository, never()).findById(any());
        verify(surfaceFormRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should throw exception for null formNative")
    void create_ShouldThrowExceptionForNullFormNative() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "lemma-123", null, "namaskara", "oblique", "Notes"
        );
        String actor = "admin@example.com";
        
        // No mocks needed - formNative validation happens before any repository calls

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.create(request, actor)
        );
        assertEquals("formNative must be provided", exception.getMessage());
        verify(lemmaRepository, never()).findById(any());
        verify(languageService, never()).isLanguageEnabled(any());
        verify(surfaceFormRepository, never()).existsByLemma_IdAndFormNative(any(), any());
        verify(surfaceFormRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should throw exception for blank formNative")
    void create_ShouldThrowExceptionForBlankFormNative() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "lemma-123", "   ", "namaskara", "oblique", "Notes"
        );
        String actor = "admin@example.com";
        
        // No mocks needed - formNative validation happens before any repository calls

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.create(request, actor)
        );
        assertEquals("formNative must be provided", exception.getMessage());
        verify(lemmaRepository, never()).findById(any());
        verify(languageService, never()).isLanguageEnabled(any());
        verify(surfaceFormRepository, never()).existsByLemma_IdAndFormNative(any(), any());
        verify(surfaceFormRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should throw exception for nonexistent lemma")
    void create_ShouldThrowExceptionForNonexistentLemma() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "nonexistent-lemma", "नमस्कारा", "namaskara", "oblique", "Notes"
        );
        String actor = "admin@example.com";
        
        when(lemmaRepository.findById("nonexistent-lemma")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.create(request, actor)
        );
        assertEquals("Lemma not found: nonexistent-lemma", exception.getMessage());
        verify(languageService, never()).isLanguageEnabled(any());
        verify(surfaceFormRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should throw exception for disabled language")
    void create_ShouldThrowExceptionForDisabledLanguage() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "lemma-123", "नमस्कारा", "namaskara", "oblique", "Notes"
        );
        String actor = "admin@example.com";
        
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.create(request, actor)
        );
        assertEquals("Language is not enabled or not found: mr", exception.getMessage());
        verify(surfaceFormRepository, never()).existsByLemma_IdAndFormNative(any(), any());
        verify(surfaceFormRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should throw exception for duplicate surface form")
    void create_ShouldThrowExceptionForDuplicateSurfaceForm() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "lemma-123", "नमस्कारा", "namaskara", "oblique", "Notes"
        );
        String actor = "admin@example.com";
        
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(surfaceFormRepository.existsByLemma_IdAndFormNative("lemma-123", "नमस्कारा")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.create(request, actor)
        );
        assertEquals("SurfaceForm already exists for lemmaId=lemma-123 formNative=नमस्कारा", exception.getMessage());
        verify(surfaceFormRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create - Should handle null actor gracefully")
    void create_ShouldHandleNullActorGracefully() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "lemma-123", "नमस्कारा", "namaskara", "oblique", "Notes"
        );
        
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(surfaceFormRepository.existsByLemma_IdAndFormNative("lemma-123", "नमस्कारा")).thenReturn(false);
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

        // When
        surfaceFormService.create(request, null);

        // Then
        ArgumentCaptor<SurfaceForm> formCaptor = ArgumentCaptor.forClass(SurfaceForm.class);
        verify(surfaceFormRepository).save(formCaptor.capture());
        
        SurfaceForm capturedForm = formCaptor.getValue();
        assertNull(capturedForm.getCreatedBy());
        assertNull(capturedForm.getLastModifiedBy());
    }

    // =========================================================
    // update Tests
    // =========================================================

    @Test
    @DisplayName("update - Should update surface form fields successfully")
    void update_ShouldUpdateSurfaceFormFieldsSuccessfully() {
        // Given
        String id = "surface-123";
        SurfaceFormUpdateRequest request = new SurfaceFormUpdateRequest(
                "नमस्कारांना", "namaskarana", "dative", "Dative case form"
        );
        String actor = "editor@example.com";
        
        SurfaceForm updatedForm = new SurfaceForm();
        updatedForm.setLemma(sampleLemma);
        updatedForm.setFormNative("नमस्कारांना");
        updatedForm.setFormLatin("namaskarana");
        updatedForm.setFormType("dative");
        updatedForm.setNotes("Dative case form");
        
        when(surfaceFormRepository.findById(id)).thenReturn(Optional.of(sampleSurfaceForm));
        when(surfaceFormRepository.existsByLemma_IdAndFormNative(any(), eq("नमस्कारांना"))).thenReturn(false);
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(updatedForm);

        // When
        SurfaceForm result = surfaceFormService.update(id, request, actor);

        // Then
        assertEquals(updatedForm, result);
        
        ArgumentCaptor<SurfaceForm> formCaptor = ArgumentCaptor.forClass(SurfaceForm.class);
        verify(surfaceFormRepository).save(formCaptor.capture());
        
        SurfaceForm capturedForm = formCaptor.getValue();
        assertEquals("नमस्कारांना", capturedForm.getFormNative());
        assertEquals("namaskarana", capturedForm.getFormLatin());
        assertEquals("dative", capturedForm.getFormType());
        assertEquals("Dative case form", capturedForm.getNotes());
        assertEquals(actor, capturedForm.getLastModifiedBy());

        verify(auditService).record(eq("SURFACE_FORM"), any(String.class), eq("SURFACE_FORM_UPDATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("update - Should only update non-null fields")
    void update_ShouldOnlyUpdateNonNullFields() {
        // Given
        String id = "surface-123";
        SurfaceFormUpdateRequest request = new SurfaceFormUpdateRequest(
                null, "new-latin", null, null
        );
        String actor = "editor@example.com";
        
        when(surfaceFormRepository.findById(id)).thenReturn(Optional.of(sampleSurfaceForm));
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

        // When
        surfaceFormService.update(id, request, actor);

        // Then
        ArgumentCaptor<SurfaceForm> formCaptor = ArgumentCaptor.forClass(SurfaceForm.class);
        verify(surfaceFormRepository).save(formCaptor.capture());
        
        SurfaceForm capturedForm = formCaptor.getValue();
        assertEquals("नमस्कारा", capturedForm.getFormNative()); // unchanged
        assertEquals("new-latin", capturedForm.getFormLatin()); // updated
        assertEquals("oblique", capturedForm.getFormType()); // unchanged
        assertEquals("Oblique case form", capturedForm.getNotes()); // unchanged
    }

    @Test
    @DisplayName("update - Should throw exception for duplicate after update")
    void update_ShouldThrowExceptionForDuplicateAfterUpdate() {
        // Given
        String id = "surface-123";
        SurfaceFormUpdateRequest request = new SurfaceFormUpdateRequest(
                "existing-form", null, null, null
        );
        String actor = "editor@example.com";
        
        when(surfaceFormRepository.findById(id)).thenReturn(Optional.of(sampleSurfaceForm));
        when(surfaceFormRepository.existsByLemma_IdAndFormNative(any(), eq("existing-form"))).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.update(id, request, actor)
        );
        assertTrue(exception.getMessage().contains("SurfaceForm already exists"));
        verify(surfaceFormRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - Should throw exception for blank formNative")
    void update_ShouldThrowExceptionForBlankFormNative() {
        // Given
        String id = "surface-123";
        SurfaceFormUpdateRequest request = new SurfaceFormUpdateRequest(
                "   ", null, null, null
        );
        String actor = "editor@example.com";
        
        when(surfaceFormRepository.findById(id)).thenReturn(Optional.of(sampleSurfaceForm));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.update(id, request, actor)
        );
        assertEquals("formNative cannot be blank", exception.getMessage());
        verify(surfaceFormRepository, never()).save(any());
    }

    @Test
    @DisplayName("update - Should not check uniqueness if formNative unchanged")
    void update_ShouldNotCheckUniquenessIfFormNativeUnchanged() {
        // Given
        String id = "surface-123";
        SurfaceFormUpdateRequest request = new SurfaceFormUpdateRequest(
                null, "new-latin", "new-type", null
        );
        String actor = "editor@example.com";
        
        when(surfaceFormRepository.findById(id)).thenReturn(Optional.of(sampleSurfaceForm));
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

        // When
        surfaceFormService.update(id, request, actor);

        // Then
        verify(surfaceFormRepository, never()).existsByLemma_IdAndFormNative(any(), any());
        verify(surfaceFormRepository).save(any(SurfaceForm.class));
    }

    // =========================================================
    // delete Tests
    // =========================================================

    @Test
    @DisplayName("delete - Should delete surface form successfully")
    void delete_ShouldDeleteSurfaceFormSuccessfully() {
        // Given
        String id = "surface-123";
        String actor = "admin@example.com";
        
        when(surfaceFormRepository.findById(id)).thenReturn(Optional.of(sampleSurfaceForm));

        // When
        surfaceFormService.delete(id, actor);

        // Then
        verify(surfaceFormRepository).delete(sampleSurfaceForm);
        verify(auditService).record(eq("SURFACE_FORM"), eq(id), eq("SURFACE_FORM_DELETED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("delete - Should throw exception for nonexistent surface form")
    void delete_ShouldThrowExceptionForNonexistentSurfaceForm() {
        // Given
        String id = "nonexistent";
        String actor = "admin@example.com";
        
        when(surfaceFormRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.delete(id, actor)
        );
        assertEquals("SurfaceForm not found: nonexistent", exception.getMessage());
        verify(surfaceFormRepository, never()).delete(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // =========================================================
    // Edge Cases and Complex Scenarios
    // =========================================================

    @Test
    @DisplayName("Multiple surface forms scenario - Should handle multiple forms for same lemma")
    void multipleSurfaceFormsScenario_ShouldHandleMultipleFormsForSameLemma() {
        // Given - Multiple surface forms for same lemma
        SurfaceForm form1 = new SurfaceForm();
        form1.setLemma(sampleLemma);
        form1.setFormNative("नमस्कार");
        form1.setFormType("direct");

        SurfaceForm form2 = new SurfaceForm();
        form2.setLemma(sampleLemma);
        form2.setFormNative("नमस्कारा");
        form2.setFormType("oblique");

        SurfaceForm form3 = new SurfaceForm();
        form3.setLemma(sampleLemma);
        form3.setFormNative("नमस्कारांना");
        form3.setFormType("dative");

        List<SurfaceForm> multipleForms = Arrays.asList(form1, form2, form3);
        
        when(surfaceFormRepository.findByLemma_IdOrderByFormNativeAscIdAsc("lemma-123")).thenReturn(multipleForms);

        // When
        List<SurfaceForm> result = surfaceFormService.listByLemmaId("lemma-123");

        // Then
        assertEquals(3, result.size());
        assertEquals("नमस्कार", result.get(0).getFormNative());
        assertEquals("नमस्कारा", result.get(1).getFormNative());
        assertEquals("नमस्कारांना", result.get(2).getFormNative());
        verify(surfaceFormRepository).findByLemma_IdOrderByFormNativeAscIdAsc("lemma-123");
    }

    @Test
    @DisplayName("create - Should handle Unicode content correctly")
    void create_ShouldHandleUnicodeContentCorrectly() {
        // Given
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "lemma-123", "नमस्कारांच्या", "namaskarancha", "genitive", "मराठी भाषेतील संबोधन पदाचा जेनिटिव्ह रूप"
        );
        String actor = "linguist@example.com";
        
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(surfaceFormRepository.existsByLemma_IdAndFormNative("lemma-123", "नमस्कारांच्या")).thenReturn(false);
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

        // When
        surfaceFormService.create(request, actor);

        // Then
        ArgumentCaptor<SurfaceForm> formCaptor = ArgumentCaptor.forClass(SurfaceForm.class);
        verify(surfaceFormRepository).save(formCaptor.capture());
        
        SurfaceForm capturedForm = formCaptor.getValue();
        assertEquals("नमस्कारांच्या", capturedForm.getFormNative());
        assertEquals("namaskarancha", capturedForm.getFormLatin());
        assertEquals("genitive", capturedForm.getFormType());
        assertEquals("मराठी भाषेतील संबोधन पदाचा जेनिटिव्ह रूप", capturedForm.getNotes());
    }

    @Test
    @DisplayName("Complex workflow - Should handle complete surface form lifecycle")
    void complexWorkflow_ShouldHandleCompleteSurfaceFormLifecycle() {
        // This test demonstrates a complete workflow: create → update → delete
        String formId = "surface-123";
        String actor = "admin@example.com";
        
        // 1. Create surface form
        SurfaceFormCreateRequest createRequest = new SurfaceFormCreateRequest(
                "lemma-123", "नमस्कारा", "namaskara", "oblique", "Oblique case"
        );
        
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(surfaceFormRepository.existsByLemma_IdAndFormNative("lemma-123", "नमस्कारा")).thenReturn(false);
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

        surfaceFormService.create(createRequest, actor);

        // 2. Update surface form
        SurfaceFormUpdateRequest updateRequest = new SurfaceFormUpdateRequest(
                null, "new-namaskara", "oblique_alt", "Alternative oblique form"
        );
        when(surfaceFormRepository.findById(formId)).thenReturn(Optional.of(sampleSurfaceForm));
        
        surfaceFormService.update(formId, updateRequest, actor);

        // 3. Delete surface form
        surfaceFormService.delete(formId, actor);

        // Verify all operations
        verify(surfaceFormRepository, times(2)).save(any(SurfaceForm.class)); // create + update
        verify(surfaceFormRepository).delete(sampleSurfaceForm); // delete
        verify(auditService).record(eq("SURFACE_FORM"), any(String.class), eq("SURFACE_FORM_CREATED"), eq(actor), isNull(), any(Map.class));
        verify(auditService).record(eq("SURFACE_FORM"), any(String.class), eq("SURFACE_FORM_UPDATED"), eq(actor), isNull(), any(Map.class));
        verify(auditService).record(eq("SURFACE_FORM"), eq(formId), eq("SURFACE_FORM_DELETED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("Language validation - Should validate lemma language is enabled")
    void languageValidation_ShouldValidateLemmaLanguageIsEnabled() {
        // Given - Lemma with disabled language
        Lemma disabledLangLemma = new Lemma();
        disabledLangLemma.setLanguage("disabled-lang");
        disabledLangLemma.setLemmaNative("test");
        
        SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                "lemma-disabled", "test-form", null, null, null
        );
        String actor = "admin@example.com";
        
        when(lemmaRepository.findById("lemma-disabled")).thenReturn(Optional.of(disabledLangLemma));
        when(languageService.isLanguageEnabled("disabled-lang")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.create(request, actor)
        );
        assertEquals("Language is not enabled or not found: disabled-lang", exception.getMessage());
        
        verify(languageService).isLanguageEnabled("disabled-lang");
        verify(surfaceFormRepository, never()).existsByLemma_IdAndFormNative(any(), any());
        verify(surfaceFormRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validation consistency - Should apply same validation across all methods")
    void validationConsistency_ShouldApplySameValidationAcrossAllMethods() {
        // Test that ID validation is consistent across methods
        
        // Test listByLemmaId with blank ID
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.listByLemmaId("")
        );
        assertEquals("lemmaId must be provided", exception1.getMessage());

        // Test create with blank lemma ID
        SurfaceFormCreateRequest createRequest = new SurfaceFormCreateRequest("", "form", null, null, null);
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
                surfaceFormService.create(createRequest, "actor")
        );
        assertEquals("lemmaId must be provided", exception2.getMessage());

        // Verify no repository calls were made for invalid inputs
        verify(surfaceFormRepository, never()).findByLemma_IdOrderByFormNativeAscIdAsc(any());
        verify(lemmaRepository, never()).findById(any());
        verify(surfaceFormRepository, never()).save(any());
    }

    @Test
    @DisplayName("Form type variations - Should handle different form types correctly")
    void formTypeVariations_ShouldHandleDifferentFormTypesCorrectly() {
        // Given
        String actor = "admin@example.com";
        when(lemmaRepository.findById(any())).thenReturn(Optional.of(sampleLemma));
        when(languageService.isLanguageEnabled("mr")).thenReturn(true);
        when(surfaceFormRepository.existsByLemma_IdAndFormNative(any(), any())).thenReturn(false);
        when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

        // Test different form types
        String[] formTypes = {"direct", "oblique", "dative", "genitive", "locative", "instrumental", "vocative", "plural", null, ""};
        String[] expectedTypes = {"direct", "oblique", "dative", "genitive", "locative", "instrumental", "vocative", "plural", null, null};

        for (int i = 0; i < formTypes.length; i++) {
            // Reset mock
            reset(surfaceFormRepository);
            when(surfaceFormRepository.existsByLemma_IdAndFormNative(any(), any())).thenReturn(false);
            when(surfaceFormRepository.save(any(SurfaceForm.class))).thenReturn(sampleSurfaceForm);

            SurfaceFormCreateRequest request = new SurfaceFormCreateRequest(
                    "lemma-" + i, "form-" + i, null, formTypes[i], null
            );

            // When
            surfaceFormService.create(request, actor);

            // Then
            ArgumentCaptor<SurfaceForm> formCaptor = ArgumentCaptor.forClass(SurfaceForm.class);
            verify(surfaceFormRepository).save(formCaptor.capture());
            
            SurfaceForm capturedForm = formCaptor.getValue();
            assertEquals(expectedTypes[i], capturedForm.getFormType(), 
                    "Form type mismatch for input: " + formTypes[i]);
        }
    }
}