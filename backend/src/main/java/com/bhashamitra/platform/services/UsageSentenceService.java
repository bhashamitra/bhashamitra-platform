package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.UsageSentence;
import com.bhashamitra.platform.models.UsageSentenceStatus;
import com.bhashamitra.platform.repositories.UsageSentenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsageSentenceService {

    private static final String ENTITY_TYPE = "USAGE_SENTENCE";

    private final UsageSentenceRepository usageSentenceRepository;
    private final LanguageService languageService;
    private final AuditService auditService;

    public UsageSentenceService(UsageSentenceRepository usageSentenceRepository,
                                LanguageService languageService,
                                AuditService auditService) {
        this.usageSentenceRepository = usageSentenceRepository;
        this.languageService = languageService;
        this.auditService = auditService;
    }

    // =========================================================
    // Admin/Editor use-cases (all statuses)
    // =========================================================

    @Transactional(readOnly = true)
    public UsageSentence getById(String id) {
        return usageSentenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("UsageSentence not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<UsageSentence> listByLanguage(String language) {
        String lang = normalize(language);
        requireEnabledLanguage(lang);
        return usageSentenceRepository.findByLanguageOrderBySentenceNativeAsc(lang);
    }

    @Transactional(readOnly = true)
    public List<UsageSentence> listByLanguageAndStatus(String language, UsageSentenceStatus status) {
        String lang = normalize(language);
        requireEnabledLanguage(lang);
        if (status == null) throw new IllegalArgumentException("status is required");
        return usageSentenceRepository.findByLanguageAndStatusOrderBySentenceNativeAsc(lang, status);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public UsageSentence create(UsageSentenceCreateRequest req, String actor) {
        String language = normalize(req.language());
        requireEnabledLanguage(language);

        UsageSentence s = new UsageSentence();
        s.setLanguage(language);

        s.setSentenceNative(requireNonBlank(req.sentenceNative(), "sentenceNative"));
        s.setSentenceLatin(normalizeNullable(req.sentenceLatin()));
        s.setTranslation(normalizeNullable(req.translation()));

        s.setRegister(normalizeRegister(req.register()));
        s.setExplanation(normalizeNullable(req.explanation()));
        s.setDifficulty(req.difficulty());

        s.setStatus(req.status() != null ? req.status() : UsageSentenceStatus.DRAFT);

        if (isNonBlank(actor)) {
            s.setCreatedBy(actor);
            s.setLastModifiedBy(actor);
        }

        UsageSentence saved = usageSentenceRepository.save(s);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("language", saved.getLanguage());
        details.put("register", saved.getRegister());
        details.put("difficulty", saved.getDifficulty());
        details.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);

        auditService.record(
                ENTITY_TYPE,
                saved.getId(),
                "USAGE_SENTENCE_CREATED",
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
    public UsageSentence update(String id, UsageSentenceUpdateRequest req, String actor) {
        UsageSentence existing = getById(id);

        String beforeLanguage = existing.getLanguage();
        String beforeSentenceNative = existing.getSentenceNative();
        String beforeSentenceLatin = existing.getSentenceLatin();
        String beforeTranslation = existing.getTranslation();
        String beforeRegister = existing.getRegister();
        String beforeExplanation = existing.getExplanation();
        Integer beforeDifficulty = existing.getDifficulty();

        // language change (optional; consistent with LemmaService)
        String newLanguage = req.language() != null ? normalize(req.language()) : existing.getLanguage();
        requireEnabledLanguage(newLanguage);

        if (req.sentenceNative() != null) {
            existing.setSentenceNative(requireNonBlank(req.sentenceNative(), "sentenceNative"));
        }
        if (req.sentenceLatin() != null) {
            existing.setSentenceLatin(normalizeNullable(req.sentenceLatin()));
        }
        if (req.translation() != null) {
            existing.setTranslation(normalizeNullable(req.translation()));
        }
        if (req.register() != null) {
            existing.setRegister(normalizeRegister(req.register()));
        }
        if (req.explanation() != null) {
            existing.setExplanation(normalizeNullable(req.explanation()));
        }
        if (req.difficulty() != null) {
            existing.setDifficulty(req.difficulty());
        }

        existing.setLanguage(newLanguage);

        if (isNonBlank(actor)) {
            existing.setLastModifiedBy(actor);
        }

        UsageSentence saved = usageSentenceRepository.save(existing);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("language", beforeLanguage);
        before.put("sentenceNative", beforeSentenceNative);
        before.put("sentenceLatin", beforeSentenceLatin);
        before.put("translation", beforeTranslation);
        before.put("register", beforeRegister);
        before.put("explanation", beforeExplanation);
        before.put("difficulty", beforeDifficulty);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("language", saved.getLanguage());
        after.put("sentenceNative", saved.getSentenceNative());
        after.put("sentenceLatin", saved.getSentenceLatin());
        after.put("translation", saved.getTranslation());
        after.put("register", saved.getRegister());
        after.put("explanation", saved.getExplanation());
        after.put("difficulty", saved.getDifficulty());

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("before", before);
        details.put("after", after);

        auditService.record(
                ENTITY_TYPE,
                saved.getId(),
                "USAGE_SENTENCE_UPDATED",
                actor,
                null,
                details
        );

        return saved;
    }

    // =========================================================
    // STATUS workflow
    // =========================================================

    @Transactional
    public UsageSentence setStatus(String id, UsageSentenceStatus newStatus, String actor) {
        UsageSentence s = getById(id);
        UsageSentenceStatus oldStatus = s.getStatus();

        if (newStatus == null) throw new IllegalArgumentException("Status cannot be null");

        // Guardrails (mirrors lemma behavior)
        if (s.getStatus() == UsageSentenceStatus.ARCHIVED && newStatus == UsageSentenceStatus.PUBLISHED) {
            throw new IllegalArgumentException("Cannot publish an ARCHIVED sentence. Unarchive to REVIEW first.");
        }
        if (s.getStatus() == UsageSentenceStatus.DRAFT && newStatus == UsageSentenceStatus.PUBLISHED) {
            throw new IllegalArgumentException("Cannot publish directly from DRAFT. Move to REVIEW first.");
        }

        s.setStatus(newStatus);

        if (isNonBlank(actor)) {
            s.setLastModifiedBy(actor);
        }

        UsageSentence saved = usageSentenceRepository.save(s);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("from", oldStatus != null ? oldStatus.name() : null);
        details.put("to", saved.getStatus() != null ? saved.getStatus().name() : null);

        auditService.record(
                ENTITY_TYPE,
                saved.getId(),
                "USAGE_SENTENCE_STATUS_CHANGED",
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
    public List<UsageSentence> listPublishedByLanguage(String language) {
        String lang = normalize(language);
        requireEnabledLanguage(lang);
        return usageSentenceRepository.findByLanguageAndStatusOrderBySentenceNativeAsc(
                lang,
                UsageSentenceStatus.PUBLISHED
        );
    }

    @Transactional(readOnly = true)
    public UsageSentence getPublishedById(String id) {
        UsageSentence s = getById(id);
        if (s.getStatus() != UsageSentenceStatus.PUBLISHED) {
            throw new IllegalArgumentException("Published usage sentence not found: " + id);
        }
        return s;
    }

    // =========================================================
    // Request records used by controller -> service
    // =========================================================

    public record UsageSentenceCreateRequest(
            String language,
            String sentenceNative,
            String sentenceLatin,
            String translation,
            String register,
            String explanation,
            Integer difficulty,
            UsageSentenceStatus status
    ) {}

    public record UsageSentenceUpdateRequest(
            String language,
            String sentenceNative,
            String sentenceLatin,
            String translation,
            String register,
            String explanation,
            Integer difficulty
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
        if (out.isEmpty()) return null;
        return out;
    }

    private static String normalizeNullable(String s) {
        if (s == null) return null;
        String out = s.trim();
        return out.isEmpty() ? null : out;
    }

    private static boolean isNonBlank(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private static String normalizeRegister(String v) {
        String r = normalizeNullable(v);
        if (r == null) return "neutral";
        return r.toLowerCase();
    }
}
