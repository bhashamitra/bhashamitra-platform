// ===== FILE: ./com/bhashamitra/platform/controllers/AdminLemmaSentenceLinkController.java =====
package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.CreateLemmaSentenceLinkRequest;
import com.bhashamitra.platform.controllers.dto.LemmaSentenceLinkDto;
import com.bhashamitra.platform.controllers.dto.UpdateLemmaSentenceLinkRequest;
import com.bhashamitra.platform.models.LemmaSentenceLink;
import com.bhashamitra.platform.services.LemmaSentenceLinkService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bhashamitra.platform.security.ActorUtil.actor;

@RestController
@RequestMapping("/api/admin/lemma-sentence-links")
public class AdminLemmaSentenceLinkController {

    private final LemmaSentenceLinkService service;

    public AdminLemmaSentenceLinkController(LemmaSentenceLinkService service) {
        this.service = service;
    }

    // --------------------
    // READ
    // --------------------

    @GetMapping("/{id}")
    public ResponseEntity<LemmaSentenceLinkDto> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(toDto(service.getById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List links by lemmaId OR by sentenceId.
     * Exactly one of lemmaId/sentenceId must be provided.
     */
    @GetMapping
    public ResponseEntity<List<LemmaSentenceLinkDto>> list(
            @RequestParam(required = false) String lemmaId,
            @RequestParam(required = false) String sentenceId
    ) {
        boolean hasLemma = lemmaId != null && !lemmaId.isBlank();
        boolean hasSentence = sentenceId != null && !sentenceId.isBlank();

        if (hasLemma == hasSentence) { // both true OR both false
            return ResponseEntity.badRequest().build();
        }

        List<LemmaSentenceLink> out = hasLemma
                ? service.listByLemmaId(lemmaId)
                : service.listBySentenceId(sentenceId);

        return ResponseEntity.ok(out.stream().map(AdminLemmaSentenceLinkController::toDto).toList());
    }

    // --------------------
    // CREATE
    // --------------------

    @PostMapping
    public ResponseEntity<LemmaSentenceLinkDto> create(@Valid @RequestBody CreateLemmaSentenceLinkRequest req,
                                                       Authentication auth) {
        String act = actor(auth);

        LemmaSentenceLinkService.CreateRequest svcReq = new LemmaSentenceLinkService.CreateRequest(
                req.lemmaId(),
                req.sentenceId(),
                req.surfaceFormId(),
                req.linkType()
        );

        try {
            LemmaSentenceLink created = service.create(svcReq, act);
            return ResponseEntity.ok(toDto(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // UPDATE
    // --------------------

    @PutMapping("/{id}")
    public ResponseEntity<LemmaSentenceLinkDto> update(@PathVariable String id,
                                                       @Valid @RequestBody UpdateLemmaSentenceLinkRequest req,
                                                       Authentication auth) {
        String act = actor(auth);

        LemmaSentenceLinkService.UpdateRequest svcReq = new LemmaSentenceLinkService.UpdateRequest(
                req.surfaceFormId(),
                req.linkType()
        );

        try {
            LemmaSentenceLink updated = service.update(id, svcReq, act);
            return ResponseEntity.ok(toDto(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // DELETE
    // --------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        try {
            service.delete(id, actor(auth));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --------------------
    // Helpers
    // --------------------

    private static LemmaSentenceLinkDto toDto(LemmaSentenceLink l) {
        return new LemmaSentenceLinkDto(
                l.getId(),
                l.getLemma() != null ? l.getLemma().getId() : null,
                l.getSentence() != null ? l.getSentence().getId() : null,
                l.getSurfaceFormId(),
                l.getLinkType() != null ? l.getLinkType().name() : null
        );
    }
}
