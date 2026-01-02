package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.LemmaSentenceLink;
import com.bhashamitra.platform.models.LemmaSentenceLinkType;
import com.bhashamitra.platform.models.UsageSentence;
import com.bhashamitra.platform.repositories.LemmaRepository;
import com.bhashamitra.platform.repositories.LemmaSentenceLinkRepository;
import com.bhashamitra.platform.repositories.UsageSentenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LemmaSentenceLinkService {

    private static final String ENTITY_TYPE = "LEMMA_SENTENCE_LINK";

    private final LemmaSentenceLinkRepository repository;
    private final LemmaRepository lemmaRepository;
    private final UsageSentenceRepository usageSentenceRepository;
    private final AuditService auditService;

    public LemmaSentenceLinkService(LemmaSentenceLinkRepository repository,
                                    LemmaRepository lemmaRepository,
                                    UsageSentenceRepository usageSentenceRepository,
                                    AuditService auditService) {
        this.repository = repository;
        this.lemmaRepository = lemmaRepository;
        this.usageSentenceRepository = usageSentenceRepository;
        this.auditService = auditService;
    }

    // =========================================================
    // READ
    // =========================================================

    @Transactional(readOnly = true)
    public LemmaSentenceLink getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("LemmaSentenceLink not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<LemmaSentenceLink> listByLemmaId(String lemmaId) {
        requireNonBlank(lemmaId, "lemmaId");
        return repository.findByLemma_IdOrderByCreatedDateDescIdDesc(lemmaId);
    }

    @Transactional(readOnly = true)
    public List<LemmaSentenceLink> listBySentenceId(String sentenceId) {
        requireNonBlank(sentenceId, "sentenceId");
        return repository.findBySentence_IdOrderByCreatedDateDescIdDesc(sentenceId);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public LemmaSentenceLink create(CreateRequest req, String actor) {
        String lemmaId = requireNonBlank(req.lemmaId(), "lemmaId");
        String sentenceId = requireNonBlank(req.sentenceId(), "sentenceId");

        if (repository.existsByLemma_IdAndSentence_Id(lemmaId, sentenceId)) {
            throw new IllegalArgumentException(
                    "Link already exists for lemmaId=" + lemmaId + " sentenceId=" + sentenceId
            );
        }

        Lemma lemma = lemmaRepository.findById(lemmaId)
                .orElseThrow(() -> new IllegalArgumentException("Lemma not found: " + lemmaId));

        UsageSentence sentence = usageSentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new IllegalArgumentException("UsageSentence not found: " + sentenceId));

        LemmaSentenceLink link = new LemmaSentenceLink();
        link.setLemma(lemma);
        link.setSentence(sentence);
        link.setSurfaceFormId(normalizeNullable(req.surfaceFormId()));
        link.setLinkType(resolveLinkType(req.linkType()));

        if (isNonBlank(actor)) {
            link.setCreatedBy(actor);
            link.setLastModifiedBy(actor);
        }

        LemmaSentenceLink saved = repository.save(link);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lemmaId", lemmaId);
        details.put("sentenceId", sentenceId);
        details.put("surfaceFormId", saved.getSurfaceFormId());
        details.put("linkType", saved.getLinkType().name());

        auditService.record(
                ENTITY_TYPE,
                saved.getId(),
                "LEMMA_SENTENCE_LINK_CREATED",
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
    public LemmaSentenceLink update(String id, UpdateRequest req, String actor) {
        LemmaSentenceLink existing = getById(id);

        String beforeSurfaceFormId = existing.getSurfaceFormId();
        LemmaSentenceLinkType beforeType = existing.getLinkType();

        if (req.surfaceFormId() != null) {
            existing.setSurfaceFormId(normalizeNullable(req.surfaceFormId()));
        }
        if (req.linkType() != null) {
            existing.setLinkType(resolveLinkType(req.linkType()));
        }

        if (isNonBlank(actor)) {
            existing.setLastModifiedBy(actor);
        }

        LemmaSentenceLink saved = repository.save(existing);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("surfaceFormId", beforeSurfaceFormId);
        before.put("linkType", beforeType.name());

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("surfaceFormId", saved.getSurfaceFormId());
        after.put("linkType", saved.getLinkType().name());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lemmaId", saved.getLemma().getId());
        details.put("sentenceId", saved.getSentence().getId());
        details.put("before", before);
        details.put("after", after);

        auditService.record(
                ENTITY_TYPE,
                saved.getId(),
                "LEMMA_SENTENCE_LINK_UPDATED",
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
        LemmaSentenceLink existing = getById(id);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lemmaId", existing.getLemma().getId());
        details.put("sentenceId", existing.getSentence().getId());
        details.put("surfaceFormId", existing.getSurfaceFormId());
        details.put("linkType", existing.getLinkType().name());

        repository.delete(existing);

        auditService.record(
                ENTITY_TYPE,
                id,
                "LEMMA_SENTENCE_LINK_DELETED",
                actor,
                null,
                details
        );
    }

    // =========================================================
    // Request records (controller → service)
    // =========================================================

    public record CreateRequest(
            String lemmaId,
            String sentenceId,
            String surfaceFormId,
            String linkType
    ) {}

    public record UpdateRequest(
            String surfaceFormId,
            String linkType
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

    private static String normalizeNullable(String s) {
        if (s == null) return null;
        String out = s.trim();
        return out.isEmpty() ? null : out;
    }

    private static boolean isNonBlank(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private static LemmaSentenceLinkType resolveLinkType(String v) {
        if (v == null || v.isBlank()) {
            return LemmaSentenceLinkType.EXACT;
        }
        return LemmaSentenceLinkType.valueOf(v.trim().toUpperCase());
    }
}
