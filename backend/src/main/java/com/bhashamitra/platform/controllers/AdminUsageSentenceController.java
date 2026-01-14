package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.CreateUsageSentenceRequest;
import com.bhashamitra.platform.controllers.dto.PagedSentencesResponse;
import com.bhashamitra.platform.controllers.dto.UpdateUsageSentenceRequest;
import com.bhashamitra.platform.controllers.dto.UsageSentenceDto;
import com.bhashamitra.platform.controllers.dto.UsageSentenceSearchRequest;
import com.bhashamitra.platform.models.UsageSentence;
import com.bhashamitra.platform.models.UsageSentenceStatus;
import com.bhashamitra.platform.services.UsageSentenceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<UsageSentenceDto> getById(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(toDto(usageSentenceService.getById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Search sentences with pagination, sorting, and filtering
    @GetMapping
    public PagedSentencesResponse searchSentences(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "language", defaultValue = "mr") String language,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "sentenceNative") String sort,
            @RequestParam(name = "direction", defaultValue = "asc") String direction
    ) {
        UsageSentenceSearchRequest request = new UsageSentenceSearchRequest(
                search, language, status, page, size, sort, direction
        );

        Page<UsageSentence> sentencePage = usageSentenceService.searchSentences(request);

        List<UsageSentenceDto> content = sentencePage.getContent().stream()
                .map(AdminUsageSentenceController::toDto)
                .toList();

        return new PagedSentencesResponse(
                content,
                sentencePage.getNumber(),
                sentencePage.getSize(),
                sentencePage.getTotalElements(),
                sentencePage.getTotalPages(),
                sentencePage.isFirst(),
                sentencePage.isLast()
        );
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
    public ResponseEntity<UsageSentenceDto> update(@PathVariable("id") String id,
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
    public ResponseEntity<UsageSentenceDto> setStatus(@PathVariable("id") String id,
                                                      @RequestParam("status") String status,
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
    public ResponseEntity<UsageSentenceDto> archive(@PathVariable("id") String id, Authentication auth) {
        String act = actor(auth);
        try {
            UsageSentence updated = usageSentenceService.setStatus(id, UsageSentenceStatus.ARCHIVED, act);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<UsageSentenceDto> unarchive(@PathVariable("id") String id,
                                                      @RequestParam(value = "status", defaultValue = "REVIEW") String status,
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
