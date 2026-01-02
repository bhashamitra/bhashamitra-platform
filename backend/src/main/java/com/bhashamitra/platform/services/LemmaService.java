package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.LemmaStatus;
import com.bhashamitra.platform.repositories.LemmaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LemmaService {

    private final LemmaRepository lemmaRepository;
    private final LanguageService languageService;
    private final AuditService auditService;

    public LemmaService(LemmaRepository lemmaRepository,
                        LanguageService languageService,
                        AuditService auditService) {
        this.lemmaRepository = lemmaRepository;
        this.languageService = languageService;
        this.auditService = auditService;
    }

    // =========================================================
    // Admin/Editor use-cases (all statuses)
    // =========================================================

    @Transactional(readOnly = true)
    public Lemma getById(String id) {
        return lemmaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lemma not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Lemma> listByLanguage(String language) {
        requireEnabledLanguage(language);
        return lemmaRepository.findByLanguageOrderByLemmaNativeAsc(language);
    }

    @Transactional(readOnly = true)
    public List<Lemma> listByLanguageAndStatus(String language, LemmaStatus status) {
        requireEnabledLanguage(language);
        return lemmaRepository.findByLanguageAndStatusOrderByLemmaNativeAsc(language, status);
    }

    /**
     * Create a lemma (default DRAFT unless request explicitly sets status).
     * Enforces uniqueness: (language, lemma_native)
     */
    @Transactional
    public Lemma create(LemmaCreateRequest req, String actor) {
        String language = normalize(req.language());
        String lemmaNative = normalize(req.lemmaNative());

        requireEnabledLanguage(language);

        if (lemmaRepository.existsByLanguageAndLemmaNative(language, lemmaNative)) {
            throw new IllegalArgumentException(
                    "Lemma already exists for language=" + language + " lemmaNative=" + lemmaNative
            );
        }

        Lemma lemma = new Lemma();
        lemma.setLanguage(language);
        lemma.setLemmaNative(lemmaNative);
        lemma.setLemmaLatin(normalizeNullable(req.lemmaLatin()));
        lemma.setPos(normalizeNullable(req.pos()));
        lemma.setNotes(req.notes()); // keep notes as-is (TEXT)
        lemma.setStatus(req.status() != null ? req.status() : LemmaStatus.DRAFT);

        if (actor != null && !actor.isBlank()) {
            lemma.setCreatedBy(actor);
            lemma.setLastModifiedBy(actor);
        }

        Lemma saved = lemmaRepository.save(lemma);

        // --- audit (Object details; AuditService serializes to JSON via tools.jackson ObjectMapper) ---
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("language", saved.getLanguage());
        details.put("lemmaNative", saved.getLemmaNative());
        details.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);

        auditService.record(
                "LEMMA",
                saved.getId(),
                "LEMMA_CREATED",
                actor,
                null,
                details
        );

        return saved;
    }

    /**
     * Update lemma fields. If lemmaNative changes, re-check uniqueness.
     */
    @Transactional
    public Lemma update(String id, LemmaUpdateRequest req, String actor) {
        Lemma existing = getById(id);

        String beforeLanguage = existing.getLanguage();
        String beforeLemmaNative = existing.getLemmaNative();
        String beforeLemmaLatin = existing.getLemmaLatin();
        String beforePos = existing.getPos();
        String beforeNotes = existing.getNotes();

        // If language is being changed (you may disallow this; up to you)
        String newLanguage = req.language() != null ? normalize(req.language()) : existing.getLanguage();
        requireEnabledLanguage(newLanguage);

        String newLemmaNative = req.lemmaNative() != null ? normalize(req.lemmaNative()) : existing.getLemmaNative();

        // Uniqueness check only if language/native changed
        boolean languageChanged = !existing.getLanguage().equals(newLanguage);
        boolean nativeChanged = !existing.getLemmaNative().equals(newLemmaNative);
        if (languageChanged || nativeChanged) {
            if (lemmaRepository.existsByLanguageAndLemmaNative(newLanguage, newLemmaNative)) {
                throw new IllegalArgumentException(
                        "Lemma already exists for language=" + newLanguage + " lemmaNative=" + newLemmaNative
                );
            }
        }

        existing.setLanguage(newLanguage);
        existing.setLemmaNative(newLemmaNative);

        if (req.lemmaLatin() != null) existing.setLemmaLatin(normalizeNullable(req.lemmaLatin()));
        if (req.pos() != null) existing.setPos(normalizeNullable(req.pos()));
        if (req.notes() != null) existing.setNotes(req.notes());

        if (actor != null && !actor.isBlank()) {
            existing.setLastModifiedBy(actor);
        }

        Lemma saved = lemmaRepository.save(existing);

        // --- audit (before/after) ---
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("language", beforeLanguage);
        before.put("lemmaNative", beforeLemmaNative);
        before.put("lemmaLatin", beforeLemmaLatin);
        before.put("pos", beforePos);
        before.put("notes", beforeNotes);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("language", saved.getLanguage());
        after.put("lemmaNative", saved.getLemmaNative());
        after.put("lemmaLatin", saved.getLemmaLatin());
        after.put("pos", saved.getPos());
        after.put("notes", saved.getNotes());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("before", before);
        details.put("after", after);

        auditService.record(
                "LEMMA",
                saved.getId(),
                "LEMMA_UPDATED",
                actor,
                null,
                details
        );

        return saved;
    }

    /**
     * Move lemma through workflow.
     * Typical UI actions:
     * - Submit for review: DRAFT -> REVIEW
     * - Publish: REVIEW -> PUBLISHED
     * - Unpublish: PUBLISHED -> REVIEW or DRAFT (your call)
     */
    @Transactional
    public Lemma setStatus(String id, LemmaStatus newStatus, String actor) {
        Lemma lemma = getById(id);
        LemmaStatus oldStatus = lemma.getStatus();

        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        // If archived, disallow publishing directly (force an explicit unarchive step)
        if (lemma.getStatus() == LemmaStatus.ARCHIVED && newStatus == LemmaStatus.PUBLISHED) {
            throw new IllegalArgumentException("Cannot publish an ARCHIVED lemma. Unarchive to REVIEW first.");
        }

        // Optional guardrail (keep your existing rule)
        if (lemma.getStatus() == LemmaStatus.DRAFT && newStatus == LemmaStatus.PUBLISHED) {
            throw new IllegalArgumentException("Cannot publish directly from DRAFT. Move to REVIEW first.");
        }

        lemma.setStatus(newStatus);

        if (actor != null && !actor.isBlank()) {
            lemma.setLastModifiedBy(actor);
        }

        Lemma saved = lemmaRepository.save(lemma);

        // --- audit status change (Object details) ---
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("from", oldStatus != null ? oldStatus.name() : null);
        details.put("to", saved.getStatus() != null ? saved.getStatus().name() : null);

        auditService.record(
                "LEMMA",
                saved.getId(),
                "LEMMA_STATUS_CHANGED",
                actor,
                null,
                details
        );

        return saved;
    }

    // =========================================================
    // Public use-cases (published only)
    // =========================================================

    @Transactional(readOnly = true)
    public List<Lemma> listPublishedByLanguage(String language) {
        requireEnabledLanguage(language);
        return lemmaRepository.findByLanguageAndStatusOrderByLemmaNativeAsc(language, LemmaStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public Lemma getPublishedById(String id) {
        return lemmaRepository.findByIdAndStatus(id, LemmaStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Published lemma not found: " + id));
    }

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

    private static String normalize(String s) {
        if (s == null) return null;
        String out = s.trim();
        if (out.isEmpty()) return null;
        return out;
    }

    private static String normalizeNullable(String s) {
        if (s == null) return null;
        String out = s.trim();
        return out.isEmpty() ? null : out;
    }

    // =========================================================
    // DTO placeholders (rename to your actual DTO classes)
    // =========================================================

    public record LemmaCreateRequest(
            String language,
            String lemmaNative,
            String lemmaLatin,
            String pos,
            String notes,
            LemmaStatus status
    ) {}

    public record LemmaUpdateRequest(
            String language,
            String lemmaNative,
            String lemmaLatin,
            String pos,
            String notes
    ) {}
}
