package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.CreateLemmaRequest;
import com.bhashamitra.platform.controllers.dto.LemmaDto;
import com.bhashamitra.platform.controllers.dto.UpdateLemmaRequest;
import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.LemmaStatus;
import com.bhashamitra.platform.services.LemmaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bhashamitra.platform.security.ActorUtil.actor;

@RestController
@RequestMapping("/api/admin/lemmas")
public class AdminLemmaController {

    private final LemmaService lemmaService;

    public AdminLemmaController(LemmaService lemmaService) {
        this.lemmaService = lemmaService;
    }

    // --------------------
    // READ
    // --------------------

    @GetMapping("/{id}")
    public ResponseEntity<LemmaDto> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(toDto(lemmaService.getById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // List all lemmas for a language (all statuses)
    @GetMapping
    public List<LemmaDto> listByLanguage(
            @RequestParam String language,
            @RequestParam(required = false) String status
    ) {
        List<Lemma> lemmas;

        if (status == null || status.isBlank()) {
            lemmas = lemmaService.listByLanguage(language);
        } else {
            LemmaStatus st = LemmaStatus.valueOf(status.trim().toUpperCase());
            lemmas = lemmaService.listByLanguageAndStatus(language, st);
        }

        return lemmas.stream().map(AdminLemmaController::toDto).toList();
    }

    // --------------------
    // CREATE
    // --------------------

    @PostMapping
    public ResponseEntity<LemmaDto> create(@Valid @RequestBody CreateLemmaRequest req, Authentication auth) {
        String actor = actor(auth);

        LemmaService.LemmaCreateRequest svcReq = new LemmaService.LemmaCreateRequest(
                req.language(),
                req.lemmaNative(),
                req.lemmaLatin(),
                req.pos(),
                req.notes(),
                null // default DRAFT in service
        );

        try {
            Lemma created = lemmaService.create(svcReq, actor);
            return ResponseEntity.ok(toDto(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // UPDATE (fields)
    // --------------------

    @PutMapping("/{id}")
    public ResponseEntity<LemmaDto> update(@PathVariable String id,
                                           @Valid @RequestBody UpdateLemmaRequest req,
                                           Authentication auth) {
        String actor = actor(auth);

        LemmaService.LemmaUpdateRequest svcReq = new LemmaService.LemmaUpdateRequest(
                null, // language: keep null unless you want to allow changing it
                req.lemmaNative(),
                req.lemmaLatin(),
                req.pos(),
                req.notes()
        );

        try {
            Lemma updated = lemmaService.update(id, svcReq, actor);

            // Optional status change via UpdateLemmaRequest.status
            if (req.status() != null && !req.status().isBlank()) {
                LemmaStatus st = LemmaStatus.valueOf(req.status().trim().toUpperCase());
                updated = lemmaService.setStatus(id, st, actor);
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
    public ResponseEntity<LemmaDto> setStatus(@PathVariable String id,
                                              @RequestParam String status,
                                              Authentication auth) {
        String actor = actor(auth);

        try {
            LemmaStatus st = LemmaStatus.valueOf(status.trim().toUpperCase());
            Lemma updated = lemmaService.setStatus(id, st, actor);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<LemmaDto> archive(@PathVariable String id, Authentication auth) {
        String actor = actor(auth);
        try {
            Lemma updated = lemmaService.setStatus(id, LemmaStatus.ARCHIVED, actor);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<LemmaDto> unarchive(@PathVariable String id,
                                              @RequestParam(defaultValue = "REVIEW") String status,
                                              Authentication auth) {
        String actor = actor(auth);
        try {
            LemmaStatus st = LemmaStatus.valueOf(status.trim().toUpperCase());
            if (st == LemmaStatus.ARCHIVED) {
                return ResponseEntity.badRequest().build();
            }
            Lemma updated = lemmaService.setStatus(id, st, actor);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // Helpers
    // --------------------

    private static LemmaDto toDto(Lemma l) {
        return new LemmaDto(
                l.getId(),
                l.getLanguage(),
                l.getLemmaNative(),
                l.getLemmaLatin(),
                l.getPos(),
                l.getNotes(),
                l.getStatus() != null ? l.getStatus().name() : null
        );
    }
}
