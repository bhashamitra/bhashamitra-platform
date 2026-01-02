package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.Meaning;
import com.bhashamitra.platform.repositories.LemmaRepository;
import com.bhashamitra.platform.repositories.MeaningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MeaningService {

    private final MeaningRepository meaningRepository;
    private final LemmaRepository lemmaRepository;
    private final AuditService auditService;

    public MeaningService(MeaningRepository meaningRepository,
                          LemmaRepository lemmaRepository,
                          AuditService auditService) {
        this.meaningRepository = meaningRepository;
        this.lemmaRepository = lemmaRepository;
        this.auditService = auditService;
    }

    // =========================================================
    // READ
    // =========================================================

    @Transactional(readOnly = true)
    public Meaning getById(String id) {
        return meaningRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meaning not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Meaning> listByLemmaId(String lemmaId) {
        String lid = requireNonBlank(lemmaId, "lemmaId");
        return meaningRepository.findByLemma_IdOrderByPriorityAscIdAsc(lid);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public Meaning create(MeaningCreateRequest req, String actor) {
        String lemmaId = requireNonBlank(req.lemmaId(), "lemmaId");
        String meaningLanguage = requireNonBlank(req.meaningLanguage(), "meaningLanguage").toLowerCase();
        String meaningText = requireNonBlank(req.meaningText(), "meaningText");
        Integer priority = requireNonNull(req.priority(), "priority");

        Lemma lemma = lemmaRepository.findById(lemmaId)
                .orElseThrow(() -> new IllegalArgumentException("Lemma not found: " + lemmaId));

        if (meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(lemmaId, meaningLanguage, priority)) {
            throw new IllegalArgumentException(
                    "Meaning already exists for lemmaId=" + lemmaId +
                            " meaningLanguage=" + meaningLanguage +
                            " priority=" + priority
            );
        }

        Meaning m = new Meaning();
        m.setLemma(lemma);
        m.setMeaningLanguage(meaningLanguage);
        m.setMeaningText(meaningText);
        m.setPriority(priority);

        if (isNonBlank(actor)) {
            m.setCreatedBy(actor);
            m.setLastModifiedBy(actor);
        }

        Meaning saved = meaningRepository.save(m);

        // --- audit (Object details; AuditService serializes via tools.jackson ObjectMapper) ---
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lemmaId", saved.getLemma().getId());
        details.put("meaningLanguage", saved.getMeaningLanguage());
        details.put("priority", saved.getPriority());

        auditService.record(
                "MEANING",
                saved.getId(),
                "MEANING_CREATED",
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
    public Meaning update(String id, MeaningUpdateRequest req, String actor) {
        Meaning existing = getById(id);
        String lemmaId = existing.getLemma().getId();
        String beforeLang = existing.getMeaningLanguage();
        String beforeText = existing.getMeaningText();
        Integer beforePriority = existing.getPriority();

        String newMeaningLanguage = req.meaningLanguage() != null
                ? requireNonBlank(req.meaningLanguage(), "meaningLanguage").toLowerCase()
                : existing.getMeaningLanguage();

        String newMeaningText = req.meaningText() != null
                ? requireNonBlank(req.meaningText(), "meaningText")
                : existing.getMeaningText();

        Integer newPriority = req.priority() != null ? req.priority() : existing.getPriority();
        if (newPriority == null) {
            throw new IllegalArgumentException("priority must be provided");
        }

        boolean languageChanged = !existing.getMeaningLanguage().equals(newMeaningLanguage);
        boolean priorityChanged = !existing.getPriority().equals(newPriority);

        if (languageChanged || priorityChanged) {
            boolean conflict = meaningRepository.existsByLemma_IdAndMeaningLanguageAndPriority(
                    lemmaId, newMeaningLanguage, newPriority
            );
            if (conflict) {
                throw new IllegalArgumentException(
                        "Another meaning already exists for lemmaId=" + lemmaId +
                                " meaningLanguage=" + newMeaningLanguage +
                                " priority=" + newPriority
                );
            }
        }

        existing.setMeaningLanguage(newMeaningLanguage);
        existing.setMeaningText(newMeaningText);
        existing.setPriority(newPriority);

        if (isNonBlank(actor)) {
            existing.setLastModifiedBy(actor);
        }

        Meaning saved = meaningRepository.save(existing);

        // --- audit (before/after) ---
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("meaningLanguage", beforeLang);
        before.put("meaningText", beforeText);
        before.put("priority", beforePriority);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("meaningLanguage", saved.getMeaningLanguage());
        after.put("meaningText", saved.getMeaningText());
        after.put("priority", saved.getPriority());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lemmaId", lemmaId);
        details.put("before", before);
        details.put("after", after);

        auditService.record(
                "MEANING",
                saved.getId(),
                "MEANING_UPDATED",
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
        Meaning existing = meaningRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Meaning not found: " + id));

        String lemmaId = existing.getLemma().getId();
        String lang = existing.getMeaningLanguage();
        Integer priority = existing.getPriority();

        meaningRepository.delete(existing);

        // --- audit (Object details) ---
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lemmaId", lemmaId);
        details.put("meaningLanguage", lang);
        details.put("priority", priority);

        auditService.record(
                "MEANING",
                id,
                "MEANING_DELETED",
                actor,
                null,
                details
        );
    }

    // =========================================================
    // Request records used by controller -> service
    // (keeps service independent of controller package)
    // =========================================================

    public record MeaningCreateRequest(
            String lemmaId,
            String meaningLanguage,
            String meaningText,
            Integer priority
    ) {}

    public record MeaningUpdateRequest(
            String meaningLanguage,
            String meaningText,
            Integer priority
    ) {}

    // =========================================================
    // Helpers
    // =========================================================

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must be provided");
        }
        return v.trim();
    }

    private static <T> T requireNonNull(T v, String field) {
        if (v == null) {
            throw new IllegalArgumentException(field + " must be provided");
        }
        return v;
    }

    private static boolean isNonBlank(String v) {
        return v != null && !v.trim().isEmpty();
    }
}
