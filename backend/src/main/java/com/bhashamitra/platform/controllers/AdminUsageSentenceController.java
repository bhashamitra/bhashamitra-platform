package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.CreateUsageSentenceRequest;
import com.bhashamitra.platform.controllers.dto.UpdateUsageSentenceRequest;
import com.bhashamitra.platform.controllers.dto.UsageSentenceDto;
import com.bhashamitra.platform.models.UsageSentence;
import com.bhashamitra.platform.models.UsageSentenceStatus;
import com.bhashamitra.platform.services.UsageSentenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bhashamitra.platform.security.ActorUtil.actor;

@RestController
@RequestMapping("/api/admin/sentences")
public class AdminUsageSentenceController {

    private final UsageSentenceService usageSentenceService;

    public AdminUsageSentenceController(UsageSentenceService usageSentenceService) {
        this.usageSentenceService = usageSentenceService;
    }

    // --------------------
    // READ
    // --------------------

    @GetMapping("/{id}")
    public ResponseEntity<UsageSentenceDto> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(toDto(usageSentenceService.getById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // List all sentences for a language (all statuses)
    @GetMapping
    public List<UsageSentenceDto> listByLanguage(
            @RequestParam String language,
            @RequestParam(required = false) String status
    ) {
        List<UsageSentence> out;

        if (status == null || status.isBlank()) {
            out = usageSentenceService.listByLanguage(language);
        } else {
            UsageSentenceStatus st = UsageSentenceStatus.valueOf(status.trim().toUpperCase());
            out = usageSentenceService.listByLanguageAndStatus(language, st);
        }

        return out.stream().map(AdminUsageSentenceController::toDto).toList();
    }

    // --------------------
    // CREATE
    // --------------------

    @PostMapping
    public ResponseEntity<UsageSentenceDto> create(@Valid @RequestBody CreateUsageSentenceRequest req,
                                                   Authentication auth) {
        String act = actor(auth);

        UsageSentenceService.UsageSentenceCreateRequest svcReq =
                new UsageSentenceService.UsageSentenceCreateRequest(
                        req.language(),
                        req.sentenceNative(),
                        req.sentenceLatin(),
                        req.translation(),
                        req.register(),
                        req.explanation(),
                        req.difficulty(),
                        null // default DRAFT in service
                );

        try {
            UsageSentence created = usageSentenceService.create(svcReq, act);
            return ResponseEntity.ok(toDto(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // UPDATE (fields + optional status)
    // --------------------

    @PutMapping("/{id}")
    public ResponseEntity<UsageSentenceDto> update(@PathVariable String id,
                                                   @Valid @RequestBody UpdateUsageSentenceRequest req,
                                                   Authentication auth) {
        String act = actor(auth);

        UsageSentenceService.UsageSentenceUpdateRequest svcReq =
                new UsageSentenceService.UsageSentenceUpdateRequest(
                        req.language(),
                        req.sentenceNative(),
                        req.sentenceLatin(),
                        req.translation(),
                        req.register(),
                        req.explanation(),
                        req.difficulty()
                );

        try {
            UsageSentence updated = usageSentenceService.update(id, svcReq, act);

            if (req.status() != null && !req.status().isBlank()) {
                UsageSentenceStatus st = UsageSentenceStatus.valueOf(req.status().trim().toUpperCase());
                updated = usageSentenceService.setStatus(id, st, act);
            }

            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // STATUS only (recommended for UI buttons)
    // --------------------

    @PutMapping("/{id}/status")
    public ResponseEntity<UsageSentenceDto> setStatus(@PathVariable String id,
                                                      @RequestParam String status,
                                                      Authentication auth) {
        String act = actor(auth);

        try {
            UsageSentenceStatus st = UsageSentenceStatus.valueOf(status.trim().toUpperCase());
            UsageSentence updated = usageSentenceService.setStatus(id, st, act);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<UsageSentenceDto> archive(@PathVariable String id, Authentication auth) {
        String act = actor(auth);
        try {
            UsageSentence updated = usageSentenceService.setStatus(id, UsageSentenceStatus.ARCHIVED, act);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<UsageSentenceDto> unarchive(@PathVariable String id,
                                                      @RequestParam(defaultValue = "REVIEW") String status,
                                                      Authentication auth) {
        String act = actor(auth);
        try {
            UsageSentenceStatus st = UsageSentenceStatus.valueOf(status.trim().toUpperCase());
            if (st == UsageSentenceStatus.ARCHIVED) {
                return ResponseEntity.badRequest().build();
            }
            UsageSentence updated = usageSentenceService.setStatus(id, st, act);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // Helpers
    // --------------------

    private static UsageSentenceDto toDto(UsageSentence s) {
        return new UsageSentenceDto(
                s.getId(),
                s.getLanguage(),
                s.getSentenceNative(),
                s.getSentenceLatin(),
                s.getTranslation(),
                s.getRegister(),
                s.getExplanation(),
                s.getDifficulty(),
                s.getStatus() != null ? s.getStatus().name() : null
        );
    }
}
