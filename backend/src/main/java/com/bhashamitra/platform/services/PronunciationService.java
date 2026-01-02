package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Pronunciation;
import com.bhashamitra.platform.models.UsageSentenceStatus;
import com.bhashamitra.platform.repositories.PronunciationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PronunciationService {

    private static final String ENTITY_TYPE = "PRONUNCIATION";

    private final PronunciationRepository pronunciationRepository;
    private final LemmaService lemmaService;
    private final UsageSentenceService usageSentenceService;
    private final AuditService auditService;

    public PronunciationService(PronunciationRepository pronunciationRepository,
                                LemmaService lemmaService,
                                UsageSentenceService usageSentenceService,
                                AuditService auditService) {
        this.pronunciationRepository = pronunciationRepository;
        this.lemmaService = lemmaService;
        this.usageSentenceService = usageSentenceService;
        this.auditService = auditService;
    }

    // =========================================================
    // READ
    // =========================================================

    @Transactional(readOnly = true)
    public Pronunciation getById(String id) {
        return pronunciationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pronunciation not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Pronunciation> listByOwner(String ownerType, String ownerId) {
        String ot = normalizeOwnerType(ownerType);
        String oid = requireNonBlank(ownerId, "ownerId");
        return pronunciationRepository.findByOwnerTypeAndOwnerIdOrderByCreatedDateDescIdDesc(ot, oid);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public Pronunciation create(CreateRequest req, String actor) {
        String ownerType = normalizeOwnerType(req.ownerType());
        String ownerId = requireNonBlank(req.ownerId(), "ownerId");
        String audioUri = requireNonBlank(req.audioUri(), "audioUri");

        // Validate owner exists (admin/editor may attach to DRAFT/REVIEW too)
        requireOwnerExists(ownerType, ownerId);

        // Lightweight duplicate guard (same owner + same audio uri)
        if (pronunciationRepository.existsByOwnerTypeAndOwnerIdAndAudioUri(ownerType, ownerId, audioUri)) {
            throw new IllegalArgumentException("Pronunciation already exists for ownerType=" + ownerType +
                    " ownerId=" + ownerId + " audioUri=" + audioUri);
        }

        Pronunciation p = new Pronunciation();
        p.setOwnerType(ownerType);
        p.setOwnerId(ownerId);
        p.setSpeaker(normalizeNullable(req.speaker()));
        p.setRegion(normalizeNullable(req.region()));
        p.setAudioUri(audioUri);
        p.setDurationMs(req.durationMs());

        if (isNonBlank(actor)) {
            p.setCreatedBy(actor);
            p.setLastModifiedBy(actor);
        }

        Pronunciation saved = pronunciationRepository.save(p);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("ownerType", saved.getOwnerType());
        details.put("ownerId", saved.getOwnerId());
        details.put("audioUri", saved.getAudioUri());
        details.put("speaker", saved.getSpeaker());
        details.put("region", saved.getRegion());
        details.put("durationMs", saved.getDurationMs());

        auditService.record(
                ENTITY_TYPE,
                saved.getId(),
                "PRONUNCIATION_CREATED",
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
    public Pronunciation update(String id, UpdateRequest req, String actor) {
        Pronunciation existing = getById(id);

        String beforeSpeaker = existing.getSpeaker();
        String beforeRegion = existing.getRegion();
        String beforeAudioUri = existing.getAudioUri();
        Integer beforeDuration = existing.getDurationMs();

        if (req.speaker() != null) existing.setSpeaker(normalizeNullable(req.speaker()));
        if (req.region() != null) existing.setRegion(normalizeNullable(req.region()));
        if (req.audioUri() != null) existing.setAudioUri(requireNonBlank(req.audioUri(), "audioUri"));
        if (req.durationMs() != null) existing.setDurationMs(req.durationMs());

        if (isNonBlank(actor)) {
            existing.setLastModifiedBy(actor);
        }

        Pronunciation saved = pronunciationRepository.save(existing);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("speaker", beforeSpeaker);
        before.put("region", beforeRegion);
        before.put("audioUri", beforeAudioUri);
        before.put("durationMs", beforeDuration);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("speaker", saved.getSpeaker());
        after.put("region", saved.getRegion());
        after.put("audioUri", saved.getAudioUri());
        after.put("durationMs", saved.getDurationMs());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("ownerType", saved.getOwnerType());
        details.put("ownerId", saved.getOwnerId());
        details.put("before", before);
        details.put("after", after);

        auditService.record(
                ENTITY_TYPE,
                saved.getId(),
                "PRONUNCIATION_UPDATED",
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
        Pronunciation existing = getById(id);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("ownerType", existing.getOwnerType());
        details.put("ownerId", existing.getOwnerId());
        details.put("audioUri", existing.getAudioUri());

        pronunciationRepository.delete(existing);

        auditService.record(
                ENTITY_TYPE,
                id,
                "PRONUNCIATION_DELETED",
                actor,
                null,
                details
        );
    }

    // =========================================================
    // Public guardrails
    // =========================================================

    /**
     * For public access:
     * - LEMMA owner must be PUBLISHED
     * - SENTENCE owner must be PUBLISHED
     */
    @Transactional(readOnly = true)
    public List<Pronunciation> listPublicByOwner(String ownerType, String ownerId) {
        String ot = normalizeOwnerType(ownerType);
        String oid = requireNonBlank(ownerId, "ownerId");

        if ("LEMMA".equals(ot)) {
            lemmaService.getPublishedById(oid); // throws if not published/not found
        } else if ("SENTENCE".equals(ot)) {
            usageSentenceService.getPublishedById(oid); // throws if not published/not found
        } else {
            throw new IllegalArgumentException("Unsupported ownerType for public access: " + ot);
        }

        return pronunciationRepository.findByOwnerTypeAndOwnerIdOrderByCreatedDateDescIdDesc(ot, oid);
    }

    // =========================================================
    // Request records (controller → service)
    // =========================================================

    public record CreateRequest(
            String ownerType,
            String ownerId,
            String speaker,
            String region,
            String audioUri,
            Integer durationMs
    ) {}

    public record UpdateRequest(
            String speaker,
            String region,
            String audioUri,
            Integer durationMs
    ) {}

    // =========================================================
    // Helpers
    // =========================================================

    private void requireOwnerExists(String ownerType, String ownerId) {
        if ("LEMMA".equals(ownerType)) {
            lemmaService.getById(ownerId); // admin may attach to DRAFT/REVIEW
            return;
        }
        if ("SENTENCE".equals(ownerType)) {
            usageSentenceService.getById(ownerId); // admin may attach to DRAFT/REVIEW
            return;
        }
        throw new IllegalArgumentException("Unsupported ownerType: " + ownerType);
    }

    private static String normalizeOwnerType(String ownerType) {
        String ot = requireNonBlank(ownerType, "ownerType").trim().toUpperCase();
        return ot;
    }

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
}
