package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.SurfaceForm;
import com.bhashamitra.platform.repositories.LemmaRepository;
import com.bhashamitra.platform.repositories.SurfaceFormRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SurfaceFormService {

    private static final String ENTITY_TYPE = "SURFACE_FORM";

    private final SurfaceFormRepository surfaceFormRepository;
    private final LemmaRepository lemmaRepository;
    private final LanguageService languageService;
    private final AuditService auditService;

    public SurfaceFormService(SurfaceFormRepository surfaceFormRepository,
                              LemmaRepository lemmaRepository,
                              LanguageService languageService,
                              AuditService auditService) {
        this.surfaceFormRepository = surfaceFormRepository;
        this.lemmaRepository = lemmaRepository;
        this.languageService = languageService;
        this.auditService = auditService;
    }

    // =========================================================
    // READ
    // =========================================================

    @Transactional(readOnly = true)
    public SurfaceForm getById(String id) {
        return surfaceFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SurfaceForm not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<SurfaceForm> listByLemmaId(String lemmaId) {
        String lid = requireNonBlank(lemmaId, "lemmaId");
        return surfaceFormRepository.findByLemma_IdOrderByFormNativeAscIdAsc(lid);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public SurfaceForm create(SurfaceFormCreateRequest req, String actor) {
        String lemmaId = requireNonBlank(req.lemmaId(), "lemmaId");
        String formNative = normalize(req.formNative());
        if (formNative == null) throw new IllegalArgumentException("formNative must be provided");

        Lemma lemma = lemmaRepository.findById(lemmaId)
                .orElseThrow(() -> new IllegalArgumentException("Lemma not found: " + lemmaId));

        // Ensure lemma language is enabled (same spirit as your other services)
        requireEnabledLanguage(lemma.getLanguage());

        if (surfaceFormRepository.existsByLemma_IdAndFormNative(lemmaId, formNative)) {
            throw new IllegalArgumentException(
                    "SurfaceForm already exists for lemmaId=" + lemmaId + " formNative=" + formNative
            );
        }

        SurfaceForm sf = new SurfaceForm();
        sf.setLemma(lemma);
        sf.setFormNative(formNative);
        sf.setFormLatin(normalizeNullable(req.formLatin()));
        sf.setFormType(normalizeNullable(req.formType()));
        sf.setNotes(req.notes());

        if (isNonBlank(actor)) {
            sf.setCreatedBy(actor);
            sf.setLastModifiedBy(actor);
        }

        SurfaceForm saved = surfaceFormRepository.save(sf);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lemmaId", lemmaId);
        details.put("formNative", saved.getFormNative());
        details.put("formType", saved.getFormType());

        auditService.record(
                ENTITY_TYPE,
                saved.getId(),
                "SURFACE_FORM_CREATED",
                actor,
                null,
                details
        );

        return saved;
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Transactional
    public SurfaceForm update(String id, SurfaceFormUpdateRequest req, String actor) {
        SurfaceForm existing = getById(id);

        String lemmaId = existing.getLemma().getId();
        String beforeNative = existing.getFormNative();
        String beforeLatin = existing.getFormLatin();
        String beforeType = existing.getFormType();
        String beforeNotes = existing.getNotes();

        // Optional uniqueness check if native changes
        if (req.formNative() != null) {
            String newNative = normalize(req.formNative());
            if (newNative == null) throw new IllegalArgumentException("formNative cannot be blank");

            if (!beforeNative.equals(newNative)
                    && surfaceFormRepository.existsByLemma_IdAndFormNative(lemmaId, newNative)) {
                throw new IllegalArgumentException(
                        "SurfaceForm already exists for lemmaId=" + lemmaId + " formNative=" + newNative
                );
            }
            existing.setFormNative(newNative);
        }

        if (req.formLatin() != null) existing.setFormLatin(normalizeNullable(req.formLatin()));
        if (req.formType() != null) existing.setFormType(normalizeNullable(req.formType()));
        if (req.notes() != null) existing.setNotes(req.notes());

        if (isNonBlank(actor)) {
            existing.setLastModifiedBy(actor);
        }

        SurfaceForm saved = surfaceFormRepository.save(existing);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("formNative", beforeNative);
        before.put("formLatin", beforeLatin);
        before.put("formType", beforeType);
        before.put("notes", beforeNotes);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("formNative", saved.getFormNative());
        after.put("formLatin", saved.getFormLatin());
        after.put("formType", saved.getFormType());
        after.put("notes", saved.getNotes());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lemmaId", lemmaId);
        details.put("before", before);
        details.put("after", after);

        auditService.record(
                ENTITY_TYPE,
                saved.getId(),
                "SURFACE_FORM_UPDATED",
                actor,
                null,
                details
        );

        return saved;
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void delete(String id, String actor) {
        SurfaceForm existing = getById(id);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lemmaId", existing.getLemma().getId());
        details.put("formNative", existing.getFormNative());
        details.put("formType", existing.getFormType());

        surfaceFormRepository.delete(existing);

        auditService.record(
                ENTITY_TYPE,
                id,
                "SURFACE_FORM_DELETED",
                actor,
                null,
                details
        );
    }

    // =========================================================
    // Request records (controller → service)
    // =========================================================

    public record SurfaceFormCreateRequest(
            String lemmaId,
            String formNative,
            String formLatin,
            String formType,
            String notes
    ) {}

    public record SurfaceFormUpdateRequest(
            String formNative,
            String formLatin,
            String formType,
            String notes
    ) {}

    // =========================================================
    // Helpers
    // =========================================================

    private void requireEnabledLanguage(String language) {
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("language is required");
        }
        if (!languageService.isLanguageEnabled(language)) {
            throw new IllegalArgumentException("Language is not enabled or not found: " + language);
        }
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must be provided");
        }
        return v.trim();
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String out = s.trim();
        return out.isEmpty() ? null : out;
    }

    private static String normalizeNullable(String s) {
        if (s == null) return null;
        String out = s.trim();
        return out.isEmpty() ? null : out;
    }

    private static boolean isNonBlank(String v) {
        return v != null && !v.trim().isEmpty();
    }
}
