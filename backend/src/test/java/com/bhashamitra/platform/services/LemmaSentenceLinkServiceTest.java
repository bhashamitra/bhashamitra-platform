package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.LemmaStatus;
import com.bhashamitra.platform.models.LemmaSentenceLink;
import com.bhashamitra.platform.models.LemmaSentenceLinkType;
import com.bhashamitra.platform.models.Meaning;
import com.bhashamitra.platform.models.SurfaceForm;
import com.bhashamitra.platform.models.UsageSentence;
import com.bhashamitra.platform.models.UsageSentenceStatus;
import com.bhashamitra.platform.repositories.LemmaRepository;
import com.bhashamitra.platform.repositories.LemmaSentenceLinkRepository;
import com.bhashamitra.platform.repositories.MeaningRepository;
import com.bhashamitra.platform.repositories.SurfaceFormRepository;
import com.bhashamitra.platform.repositories.UsageSentenceRepository;
import com.bhashamitra.platform.services.LemmaSentenceLinkService.CreateRequest;
import com.bhashamitra.platform.services.LemmaSentenceLinkService.UpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LemmaSentenceLinkService Tests")
class LemmaSentenceLinkServiceTest {

    @Mock
    private LemmaSentenceLinkRepository repository;

    @Mock
    private LemmaRepository lemmaRepository;

    @Mock
    private UsageSentenceRepository usageSentenceRepository;

    @Mock
    private MeaningRepository meaningRepository;

    @Mock
    private SurfaceFormRepository surfaceFormRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private LemmaSentenceLinkService linkService;

    private LemmaSentenceLink sampleLink;
    private Lemma sampleLemma;
    private UsageSentence sampleSentence;
    private SurfaceForm sampleSurfaceForm;
    private Meaning sampleMeaning;

    @BeforeEach
    void setUp() {
        // Setup sample lemma
        sampleLemma = new Lemma();
        setEntityId(sampleLemma, "lemma-123"); // Set ID for validation checks
        sampleLemma.setLanguage("mr");
        sampleLemma.setLemmaNative("नमस्कार");
        sampleLemma.setLemmaLatin("namaskar");
        sampleLemma.setPos("noun");
        sampleLemma.setStatus(LemmaStatus.PUBLISHED);

        // Setup sample usage sentence
        sampleSentence = new UsageSentence();
        setEntityId(sampleSentence, "sentence-123"); // Set ID for validation checks
        sampleSentence.setLanguage("mr");
        sampleSentence.setSentenceNative("मी तुम्हाला नमस्कार करतो.");
        sampleSentence.setSentenceLatin("Mi tumhala namaskar karto.");
        sampleSentence.setTranslation("I greet you.");
        sampleSentence.setRegister("neutral");
        sampleSentence.setStatus(UsageSentenceStatus.PUBLISHED);

        // Setup sample surface form
        sampleSurfaceForm = new SurfaceForm();
        sampleSurfaceForm.setLemma(sampleLemma);
        sampleSurfaceForm.setFormNative("नमस्कार");
        sampleSurfaceForm.setFormLatin("namaskar");
        sampleSurfaceForm.setFormType("infinitive");

        // Setup sample meaning
        sampleMeaning = new Meaning();
        sampleMeaning.setLemma(sampleLemma);
        sampleMeaning.setMeaningLanguage("en");
        sampleMeaning.setMeaningText("greeting");
        sampleMeaning.setPriority(1);

        // Setup sample link
        sampleLink = new LemmaSentenceLink();
        sampleLink.setLemma(sampleLemma);
        sampleLink.setSentence(sampleSentence);
        sampleLink.setSurfaceFormId("surface-123");
        sampleLink.setLinkType(LemmaSentenceLinkType.EXACT);
        sampleLink.setCreatedBy("admin@example.com");
        sampleLink.setLastModifiedBy("admin@example.com");
    }

    // =========================================================
    // getById Tests
    // =========================================================

    @Test
    @DisplayName("getById - Should return link when found")
    void getById_ShouldReturnLinkWhenFound() {
        // Given
        String id = "link-123";
        when(repository.findById(id)).thenReturn(Optional.of(sampleLink));

        // When
        LemmaSentenceLink result = linkService.getById(id);

        // Then
        assertEquals(sampleLink, result);
        verify(repository).findById(id);
    }

    @Test
    @DisplayName("getById - Should throw IllegalArgumentException when not found")
    void getById_ShouldThrowIllegalArgumentExceptionWhenNotFound() {
        // Given
        String id = "nonexistent";
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.getById(id)
        );
        assertEquals("LemmaSentenceLink not found: nonexistent", exception.getMessage());
        verify(repository).findById(id);
    }

    // =========================================================
    // listByLemmaId Tests
    // =========================================================

    @Test
    @DisplayName("listByLemmaId - Should return links for valid lemma ID")
    void listByLemmaId_ShouldReturnLinksForValidLemmaId() {
        // Given
        String lemmaId = "lemma-123";
        List<LemmaSentenceLink> expectedLinks = Arrays.asList(sampleLink);
        
        when(repository.findByLemma_IdOrderByCreatedDateDescIdDesc(lemmaId)).thenReturn(expectedLinks);

        // When
        List<LemmaSentenceLink> result = linkService.listByLemmaId(lemmaId);

        // Then
        assertEquals(expectedLinks, result);
        verify(repository).findByLemma_IdOrderByCreatedDateDescIdDesc(lemmaId);
    }

    @Test
    @DisplayName("listByLemmaId - Should throw exception for null lemma ID")
    void listByLemmaId_ShouldThrowExceptionForNullLemmaId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.listByLemmaId(null)
        );
        assertEquals("lemmaId must be provided", exception.getMessage());
        verify(repository, never()).findByLemma_IdOrderByCreatedDateDescIdDesc(any());
    }

    @Test
    @DisplayName("listByLemmaId - Should throw exception for blank lemma ID")
    void listByLemmaId_ShouldThrowExceptionForBlankLemmaId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.listByLemmaId("   ")
        );
        assertEquals("lemmaId must be provided", exception.getMessage());
        verify(repository, never()).findByLemma_IdOrderByCreatedDateDescIdDesc(any());
    }

    @Test
    @DisplayName("listByLemmaId - Should return empty list when no links exist")
    void listByLemmaId_ShouldReturnEmptyListWhenNoLinksExist() {
        // Given
        String lemmaId = "lemma-no-links";
        List<LemmaSentenceLink> emptyLinks = Arrays.asList();
        
        when(repository.findByLemma_IdOrderByCreatedDateDescIdDesc(lemmaId)).thenReturn(emptyLinks);

        // When
        List<LemmaSentenceLink> result = linkService.listByLemmaId(lemmaId);

        // Then
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
        verify(repository).findByLemma_IdOrderByCreatedDateDescIdDesc(lemmaId);
    }

    // =========================================================
    // listBySentenceId Tests
    // =========================================================

    @Test
    @DisplayName("listBySentenceId - Should return links for valid sentence ID")
    void listBySentenceId_ShouldReturnLinksForValidSentenceId() {
        // Given
        String sentenceId = "sentence-123";
        List<LemmaSentenceLink> expectedLinks = Arrays.asList(sampleLink);
        
        when(repository.findBySentence_IdOrderByCreatedDateDescIdDesc(sentenceId)).thenReturn(expectedLinks);

        // When
        List<LemmaSentenceLink> result = linkService.listBySentenceId(sentenceId);

        // Then
        assertEquals(expectedLinks, result);
        verify(repository).findBySentence_IdOrderByCreatedDateDescIdDesc(sentenceId);
    }

    @Test
    @DisplayName("listBySentenceId - Should throw exception for null sentence ID")
    void listBySentenceId_ShouldThrowExceptionForNullSentenceId() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.listBySentenceId(null)
        );
        assertEquals("sentenceId must be provided", exception.getMessage());
        verify(repository, never()).findBySentence_IdOrderByCreatedDateDescIdDesc(any());
    }

    // =========================================================
    // create Tests
    // =========================================================

    @Test
    @DisplayName("create - Should create link with default EXACT type")
    void create_ShouldCreateLinkWithDefaultExactType() {
        // Given
        CreateRequest request = new CreateRequest(
                "lemma-123", "sentence-123", null, "surface-123", null
        );
        String actor = "admin@example.com";
        
        when(repository.existsByLemma_IdAndSentence_Id("lemma-123", "sentence-123")).thenReturn(false);
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(usageSentenceRepository.findById("sentence-123")).thenReturn(Optional.of(sampleSentence));
        when(surfaceFormRepository.findById("surface-123")).thenReturn(Optional.of(sampleSurfaceForm));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

        // When
        LemmaSentenceLink result = linkService.create(request, actor);

        // Then
        assertEquals(sampleLink, result);
        
        ArgumentCaptor<LemmaSentenceLink> linkCaptor = ArgumentCaptor.forClass(LemmaSentenceLink.class);
        verify(repository).save(linkCaptor.capture());
        
        LemmaSentenceLink capturedLink = linkCaptor.getValue();
        assertEquals(sampleLemma, capturedLink.getLemma());
        assertEquals(sampleSentence, capturedLink.getSentence());
        assertEquals("surface-123", capturedLink.getSurfaceFormId());
        assertEquals(LemmaSentenceLinkType.EXACT, capturedLink.getLinkType());
        assertEquals(actor, capturedLink.getCreatedBy());
        assertEquals(actor, capturedLink.getLastModifiedBy());

        verify(auditService).record(eq("LEMMA_SENTENCE_LINK"), any(String.class), eq("LEMMA_SENTENCE_LINK_CREATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("create - Should create link with specified type")
    void create_ShouldCreateLinkWithSpecifiedType() {
        // Given
        CreateRequest request = new CreateRequest(
                "lemma-123", "sentence-123", null, null, "INFLECTED"
        );
        String actor = "admin@example.com";
        
        when(repository.existsByLemma_IdAndSentence_Id("lemma-123", "sentence-123")).thenReturn(false);
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(usageSentenceRepository.findById("sentence-123")).thenReturn(Optional.of(sampleSentence));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

        // When
        linkService.create(request, actor);

        // Then
        ArgumentCaptor<LemmaSentenceLink> linkCaptor = ArgumentCaptor.forClass(LemmaSentenceLink.class);
        verify(repository).save(linkCaptor.capture());
        
        LemmaSentenceLink capturedLink = linkCaptor.getValue();
        assertEquals(LemmaSentenceLinkType.INFLECTED, capturedLink.getLinkType());
    }

    @Test
    @DisplayName("create - Should normalize and trim input fields")
    void create_ShouldNormalizeAndTrimInputFields() {
        // Given
        CreateRequest request = new CreateRequest(
                "  lemma-123  ", "  sentence-123  ", null, "  surface-123  ", "  EXACT  "
        );
        String actor = "admin@example.com";
        
        when(repository.existsByLemma_IdAndSentence_Id("lemma-123", "sentence-123")).thenReturn(false);
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(usageSentenceRepository.findById("sentence-123")).thenReturn(Optional.of(sampleSentence));
        when(surfaceFormRepository.findById("surface-123")).thenReturn(Optional.of(sampleSurfaceForm));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

        // When
        linkService.create(request, actor);

        // Then
        verify(repository).existsByLemma_IdAndSentence_Id("lemma-123", "sentence-123");
        verify(lemmaRepository).findById("lemma-123");
        verify(usageSentenceRepository).findById("sentence-123");
        
        ArgumentCaptor<LemmaSentenceLink> linkCaptor = ArgumentCaptor.forClass(LemmaSentenceLink.class);
        verify(repository).save(linkCaptor.capture());
        
        LemmaSentenceLink capturedLink = linkCaptor.getValue();
        assertEquals("surface-123", capturedLink.getSurfaceFormId());
        assertEquals(LemmaSentenceLinkType.EXACT, capturedLink.getLinkType());
    }

    @Test
    @DisplayName("create - Should convert empty surface form ID to null")
    void create_ShouldConvertEmptySurfaceFormIdToNull() {
        // Given
        CreateRequest request = new CreateRequest(
                "lemma-123", "sentence-123", null, "   ", "EXACT"
        );
        String actor = "admin@example.com";
        
        when(repository.existsByLemma_IdAndSentence_Id("lemma-123", "sentence-123")).thenReturn(false);
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(usageSentenceRepository.findById("sentence-123")).thenReturn(Optional.of(sampleSentence));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

        // When
        linkService.create(request, actor);

        // Then
        ArgumentCaptor<LemmaSentenceLink> linkCaptor = ArgumentCaptor.forClass(LemmaSentenceLink.class);
        verify(repository).save(linkCaptor.capture());
        
        LemmaSentenceLink capturedLink = linkCaptor.getValue();
        assertNull(capturedLink.getSurfaceFormId());
    }

    @Test
    @DisplayName("create - Should throw exception for duplicate link")
    void create_ShouldThrowExceptionForDuplicateLink() {
        // Given
        CreateRequest request = new CreateRequest(
                "lemma-123", "sentence-123", null, "surface-123", "EXACT"
        );
        String actor = "admin@example.com";
        
        when(repository.existsByLemma_IdAndSentence_Id("lemma-123", "sentence-123")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.create(request, actor)
        );
        assertEquals("Link already exists for lemmaId=lemma-123 sentenceId=sentence-123", exception.getMessage());
        verify(lemmaRepository, never()).findById(any());
        verify(usageSentenceRepository, never()).findById(any());
        verify(repository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("create - Should throw exception for null lemma ID")
    void create_ShouldThrowExceptionForNullLemmaId() {
        // Given
        CreateRequest request = new CreateRequest(
                null, "sentence-123", null, "surface-123", "EXACT"
        );
        String actor = "admin@example.com";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.create(request, actor)
        );
        assertEquals("lemmaId must be provided", exception.getMessage());
        verify(repository, never()).existsByLemma_IdAndSentence_Id(any(), any());
    }

    @Test
    @DisplayName("create - Should throw exception for null sentence ID")
    void create_ShouldThrowExceptionForNullSentenceId() {
        // Given
        CreateRequest request = new CreateRequest(
                "lemma-123", null, null, "surface-123", "EXACT"
        );
        String actor = "admin@example.com";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.create(request, actor)
        );
        assertEquals("sentenceId must be provided", exception.getMessage());
        verify(repository, never()).existsByLemma_IdAndSentence_Id(any(), any());
    }

    @Test
    @DisplayName("create - Should throw exception for nonexistent lemma")
    void create_ShouldThrowExceptionForNonexistentLemma() {
        // Given
        CreateRequest request = new CreateRequest(
                "nonexistent-lemma", "sentence-123", null, "surface-123", "EXACT"
        );
        String actor = "admin@example.com";
        
        when(repository.existsByLemma_IdAndSentence_Id("nonexistent-lemma", "sentence-123")).thenReturn(false);
        when(lemmaRepository.findById("nonexistent-lemma")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.create(request, actor)
        );
        assertEquals("Lemma not found: nonexistent-lemma", exception.getMessage());
        verify(usageSentenceRepository, never()).findById(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should throw exception for nonexistent sentence")
    void create_ShouldThrowExceptionForNonexistentSentence() {
        // Given
        CreateRequest request = new CreateRequest(
                "lemma-123", "nonexistent-sentence", null, "surface-123", "EXACT"
        );
        String actor = "admin@example.com";
        
        when(repository.existsByLemma_IdAndSentence_Id("lemma-123", "nonexistent-sentence")).thenReturn(false);
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(usageSentenceRepository.findById("nonexistent-sentence")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.create(request, actor)
        );
        assertEquals("UsageSentence not found: nonexistent-sentence", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create - Should handle null actor gracefully")
    void create_ShouldHandleNullActorGracefully() {
        // Given
        CreateRequest request = new CreateRequest(
                "lemma-123", "sentence-123", null, "surface-123", "EXACT"
        );
        
        when(repository.existsByLemma_IdAndSentence_Id("lemma-123", "sentence-123")).thenReturn(false);
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(usageSentenceRepository.findById("sentence-123")).thenReturn(Optional.of(sampleSentence));
        when(surfaceFormRepository.findById("surface-123")).thenReturn(Optional.of(sampleSurfaceForm));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

        // When
        linkService.create(request, null);

        // Then
        ArgumentCaptor<LemmaSentenceLink> linkCaptor = ArgumentCaptor.forClass(LemmaSentenceLink.class);
        verify(repository).save(linkCaptor.capture());
        
        LemmaSentenceLink capturedLink = linkCaptor.getValue();
        assertNull(capturedLink.getCreatedBy());
        assertNull(capturedLink.getLastModifiedBy());
    }

    // =========================================================
    // update Tests
    // =========================================================

    @Test
    @DisplayName("update - Should update link fields successfully")
    void update_ShouldUpdateLinkFieldsSuccessfully() {
        // Given
        String id = "link-123";
        UpdateRequest request = new UpdateRequest(
                null, "new-surface-456", "IMPLIED"
        );
        String actor = "editor@example.com";
        
        SurfaceForm newSurfaceForm = new SurfaceForm();
        newSurfaceForm.setLemma(sampleLemma);
        newSurfaceForm.setFormNative("नमस्कार");
        newSurfaceForm.setFormType("infinitive");

        LemmaSentenceLink updatedLink = new LemmaSentenceLink();
        updatedLink.setLemma(sampleLemma);
        updatedLink.setSentence(sampleSentence);
        updatedLink.setSurfaceFormId("new-surface-456");
        updatedLink.setLinkType(LemmaSentenceLinkType.IMPLIED);
        
        when(repository.findById(id)).thenReturn(Optional.of(sampleLink));
        when(surfaceFormRepository.findById("new-surface-456")).thenReturn(Optional.of(newSurfaceForm));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(updatedLink);

        // When
        LemmaSentenceLink result = linkService.update(id, request, actor);

        // Then
        assertEquals(updatedLink, result);
        
        ArgumentCaptor<LemmaSentenceLink> linkCaptor = ArgumentCaptor.forClass(LemmaSentenceLink.class);
        verify(repository).save(linkCaptor.capture());
        
        LemmaSentenceLink capturedLink = linkCaptor.getValue();
        assertEquals("new-surface-456", capturedLink.getSurfaceFormId());
        assertEquals(LemmaSentenceLinkType.IMPLIED, capturedLink.getLinkType());
        assertEquals(actor, capturedLink.getLastModifiedBy());

        verify(auditService).record(eq("LEMMA_SENTENCE_LINK"), any(String.class), eq("LEMMA_SENTENCE_LINK_UPDATED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("update - Should only update non-null fields")
    void update_ShouldOnlyUpdateNonNullFields() {
        // Given
        String id = "link-123";
        UpdateRequest request = new UpdateRequest(
                null, null, "IMPLIED"
        );
        String actor = "editor@example.com";
        
        when(repository.findById(id)).thenReturn(Optional.of(sampleLink));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

        // When
        linkService.update(id, request, actor);

        // Then
        ArgumentCaptor<LemmaSentenceLink> linkCaptor = ArgumentCaptor.forClass(LemmaSentenceLink.class);
        verify(repository).save(linkCaptor.capture());
        
        LemmaSentenceLink capturedLink = linkCaptor.getValue();
        assertEquals("surface-123", capturedLink.getSurfaceFormId()); // unchanged
        assertEquals(LemmaSentenceLinkType.IMPLIED, capturedLink.getLinkType()); // updated
    }

    @Test
    @DisplayName("update - Should handle empty surface form ID by setting to null")
    void update_ShouldHandleEmptySurfaceFormIdBySettingToNull() {
        // Given
        String id = "link-123";
        UpdateRequest request = new UpdateRequest(
                null, "   ", null
        );
        String actor = "editor@example.com";
        
        when(repository.findById(id)).thenReturn(Optional.of(sampleLink));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

        // When
        linkService.update(id, request, actor);

        // Then
        ArgumentCaptor<LemmaSentenceLink> linkCaptor = ArgumentCaptor.forClass(LemmaSentenceLink.class);
        verify(repository).save(linkCaptor.capture());
        
        LemmaSentenceLink capturedLink = linkCaptor.getValue();
        assertNull(capturedLink.getSurfaceFormId());
    }

    // =========================================================
    // delete Tests
    // =========================================================

    @Test
    @DisplayName("delete - Should delete link successfully")
    void delete_ShouldDeleteLinkSuccessfully() {
        // Given
        String id = "link-123";
        String actor = "admin@example.com";
        
        when(repository.findById(id)).thenReturn(Optional.of(sampleLink));

        // When
        linkService.delete(id, actor);

        // Then
        verify(repository).delete(sampleLink);
        verify(auditService).record(eq("LEMMA_SENTENCE_LINK"), eq(id), eq("LEMMA_SENTENCE_LINK_DELETED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("delete - Should throw exception for nonexistent link")
    void delete_ShouldThrowExceptionForNonexistentLink() {
        // Given
        String id = "nonexistent";
        String actor = "admin@example.com";
        
        when(repository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                linkService.delete(id, actor)
        );
        assertEquals("LemmaSentenceLink not found: nonexistent", exception.getMessage());
        verify(repository, never()).delete(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // =========================================================
    // Link Type Resolution Tests
    // =========================================================

    @Test
    @DisplayName("create - Should handle all link type variations correctly")
    void create_ShouldHandleAllLinkTypeVariationsCorrectly() {
        // Given
        String actor = "admin@example.com";
        when(repository.existsByLemma_IdAndSentence_Id(any(), any())).thenReturn(false);
        when(lemmaRepository.findById(any())).thenReturn(Optional.of(sampleLemma));
        when(usageSentenceRepository.findById(any())).thenReturn(Optional.of(sampleSentence));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

        // Test different link type values
        String[] linkTypes = {"EXACT", "exact", "  INFLECTED  ", "implied", "", null};
        LemmaSentenceLinkType[] expectedTypes = {
                LemmaSentenceLinkType.EXACT, 
                LemmaSentenceLinkType.EXACT, 
                LemmaSentenceLinkType.INFLECTED, 
                LemmaSentenceLinkType.IMPLIED, 
                LemmaSentenceLinkType.EXACT, // empty defaults to EXACT
                LemmaSentenceLinkType.EXACT  // null defaults to EXACT
        };

        for (int i = 0; i < linkTypes.length; i++) {
            // Reset mock
            reset(repository);
            when(repository.existsByLemma_IdAndSentence_Id(any(), any())).thenReturn(false);
            when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

            CreateRequest request = new CreateRequest(
                    "lemma-" + i, "sentence-" + i, null, null, linkTypes[i]
            );

            // When
            linkService.create(request, actor);

            // Then
            ArgumentCaptor<LemmaSentenceLink> linkCaptor = ArgumentCaptor.forClass(LemmaSentenceLink.class);
            verify(repository).save(linkCaptor.capture());
            
            LemmaSentenceLink capturedLink = linkCaptor.getValue();
            assertEquals(expectedTypes[i], capturedLink.getLinkType(), 
                    "Link type mismatch for input: " + linkTypes[i]);
        }
    }

    // =========================================================
    // Edge Cases and Complex Scenarios
    // =========================================================

    @Test
    @DisplayName("Multiple links scenario - Should handle multiple links for same lemma")
    void multipleLinksScenario_ShouldHandleMultipleLinksForSameLemma() {
        // Given - Multiple links for same lemma
        LemmaSentenceLink link1 = new LemmaSentenceLink();
        link1.setLemma(sampleLemma);
        link1.setSentence(sampleSentence);
        link1.setLinkType(LemmaSentenceLinkType.EXACT);

        UsageSentence sentence2 = new UsageSentence();
        sentence2.setSentenceNative("नमस्कार हा शब्द वापरला जातो.");
        
        LemmaSentenceLink link2 = new LemmaSentenceLink();
        link2.setLemma(sampleLemma);
        link2.setSentence(sentence2);
        link2.setLinkType(LemmaSentenceLinkType.INFLECTED);

        List<LemmaSentenceLink> multipleLinks = Arrays.asList(link1, link2);
        
        when(repository.findByLemma_IdOrderByCreatedDateDescIdDesc("lemma-123")).thenReturn(multipleLinks);

        // When
        List<LemmaSentenceLink> result = linkService.listByLemmaId("lemma-123");

        // Then
        assertEquals(2, result.size());
        assertEquals(LemmaSentenceLinkType.EXACT, result.get(0).getLinkType());
        assertEquals(LemmaSentenceLinkType.INFLECTED, result.get(1).getLinkType());
        verify(repository).findByLemma_IdOrderByCreatedDateDescIdDesc("lemma-123");
    }

    @Test
    @DisplayName("Multiple links scenario - Should handle multiple links for same sentence")
    void multipleLinksScenario_ShouldHandleMultipleLinksForSameSentence() {
        // Given - Multiple links for same sentence
        Lemma lemma2 = new Lemma();
        lemma2.setLemmaNative("करतो");
        lemma2.setPos("verb");
        
        LemmaSentenceLink link1 = new LemmaSentenceLink();
        link1.setLemma(sampleLemma); // नमस्कार
        link1.setSentence(sampleSentence);
        link1.setLinkType(LemmaSentenceLinkType.EXACT);

        LemmaSentenceLink link2 = new LemmaSentenceLink();
        link2.setLemma(lemma2); // करतो
        link2.setSentence(sampleSentence);
        link2.setLinkType(LemmaSentenceLinkType.INFLECTED);

        List<LemmaSentenceLink> multipleLinks = Arrays.asList(link1, link2);
        
        when(repository.findBySentence_IdOrderByCreatedDateDescIdDesc("sentence-123")).thenReturn(multipleLinks);

        // When
        List<LemmaSentenceLink> result = linkService.listBySentenceId("sentence-123");

        // Then
        assertEquals(2, result.size());
        assertEquals("नमस्कार", result.get(0).getLemma().getLemmaNative());
        assertEquals("करतो", result.get(1).getLemma().getLemmaNative());
        verify(repository).findBySentence_IdOrderByCreatedDateDescIdDesc("sentence-123");
    }

    // Helper method to set entity ID using reflection
    private void setEntityId(Object entity, String id) {
        try {
            Field idField = entity.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set entity ID", e);
        }
    }

    @Test
    @DisplayName("Complex workflow - Should handle complete link lifecycle")
    void complexWorkflow_ShouldHandleCompleteLinkLifecycle() {
        // This test demonstrates a complete workflow: create → update → delete
        String linkId = "link-123";
        String actor = "admin@example.com";
        
        // 1. Create link
        CreateRequest createRequest = new CreateRequest(
                "lemma-123", "sentence-123", null, "surface-123", "EXACT"
        );
        
        when(repository.existsByLemma_IdAndSentence_Id("lemma-123", "sentence-123")).thenReturn(false);
        when(lemmaRepository.findById("lemma-123")).thenReturn(Optional.of(sampleLemma));
        when(usageSentenceRepository.findById("sentence-123")).thenReturn(Optional.of(sampleSentence));
        when(surfaceFormRepository.findById("surface-123")).thenReturn(Optional.of(sampleSurfaceForm));
        when(repository.save(any(LemmaSentenceLink.class))).thenReturn(sampleLink);

        linkService.create(createRequest, actor);

        // 2. Update link
        SurfaceForm newSurfaceForm = new SurfaceForm();
        newSurfaceForm.setLemma(sampleLemma);
        newSurfaceForm.setFormNative("नमस्कार");
        newSurfaceForm.setFormType("infinitive");
        
        UpdateRequest updateRequest = new UpdateRequest(null, "new-surface-456", "IMPLIED");
        when(repository.findById(linkId)).thenReturn(Optional.of(sampleLink));
        when(surfaceFormRepository.findById("new-surface-456")).thenReturn(Optional.of(newSurfaceForm));
        
        linkService.update(linkId, updateRequest, actor);

        // 3. Delete link
        linkService.delete(linkId, actor);

        // Verify all operations
        verify(repository, times(2)).save(any(LemmaSentenceLink.class)); // create + update
        verify(repository).delete(sampleLink); // delete
        verify(auditService).record(eq("LEMMA_SENTENCE_LINK"), any(String.class), eq("LEMMA_SENTENCE_LINK_CREATED"), eq(actor), isNull(), any(Map.class));
        verify(auditService).record(eq("LEMMA_SENTENCE_LINK"), any(String.class), eq("LEMMA_SENTENCE_LINK_UPDATED"), eq(actor), isNull(), any(Map.class));
        verify(auditService).record(eq("LEMMA_SENTENCE_LINK"), eq(linkId), eq("LEMMA_SENTENCE_LINK_DELETED"), eq(actor), isNull(), any(Map.class));
    }

    @Test
    @DisplayName("Validation consistency - Should apply same validation across all methods")
    void validationConsistency_ShouldApplySameValidationAcrossAllMethods() {
        // Test that ID validation is consistent across methods
        
        // Test listByLemmaId with blank ID
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, () ->
                linkService.listByLemmaId("")
        );
        assertEquals("lemmaId must be provided", exception1.getMessage());

        // Test listBySentenceId with blank ID
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, () ->
                linkService.listBySentenceId("")
        );
        assertEquals("sentenceId must be provided", exception2.getMessage());

        // Test create with blank lemma ID
        CreateRequest createRequest = new CreateRequest("", "sentence-123", null, null, null);
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () ->
                linkService.create(createRequest, "actor")
        );
        assertEquals("lemmaId must be provided", exception3.getMessage());

        // Test create with blank sentence ID
        CreateRequest createRequest2 = new CreateRequest("lemma-123", "", null, null, null);
        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class, () ->
                linkService.create(createRequest2, "actor")
        );
        assertEquals("sentenceId must be provided", exception4.getMessage());

        // Verify no repository calls were made for invalid inputs
        verify(repository, never()).findByLemma_IdOrderByCreatedDateDescIdDesc(any());
        verify(repository, never()).findBySentence_IdOrderByCreatedDateDescIdDesc(any());
        verify(repository, never()).existsByLemma_IdAndSentence_Id(any(), any());
    }
}