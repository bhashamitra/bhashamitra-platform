package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.CreatePronunciationRequest;
import com.bhashamitra.platform.controllers.dto.PronunciationDto;
import com.bhashamitra.platform.controllers.dto.UpdatePronunciationRequest;
import com.bhashamitra.platform.models.Pronunciation;
import com.bhashamitra.platform.services.PronunciationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bhashamitra.platform.security.ActorUtil.actor;

@RestController
@RequestMapping("/api/admin/pronunciations")
public class AdminPronunciationController {

    private final PronunciationService service;

    public AdminPronunciationController(PronunciationService service) {
        this.service = service;
    }

    // --------------------
    // READ
    // --------------------

    @GetMapping("/{id}")
    public ResponseEntity<PronunciationDto> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(toDto(service.getById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List pronunciations for an owner.
     * ownerType + ownerId are required.
     */
    @GetMapping
    public ResponseEntity<List<PronunciationDto>> listByOwner(
            @RequestParam String ownerType,
            @RequestParam String ownerId
    ) {
        try {
            List<Pronunciation> out = service.listByOwner(ownerType, ownerId);
            return ResponseEntity.ok(out.stream().map(AdminPronunciationController::toDto).toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // CREATE
    // --------------------

    @PostMapping
    public ResponseEntity<PronunciationDto> create(@Valid @RequestBody CreatePronunciationRequest req,
                                                   Authentication auth) {
        String act = actor(auth);

        PronunciationService.CreateRequest svcReq = new PronunciationService.CreateRequest(
                req.ownerType(),
                req.ownerId(),
                req.speaker(),
                req.region(),
                req.audioUri(),
                req.durationMs()
        );

        try {
            Pronunciation created = service.create(svcReq, act);
            return ResponseEntity.ok(toDto(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // UPDATE
    // --------------------

    @PutMapping("/{id}")
    public ResponseEntity<PronunciationDto> update(@PathVariable String id,
                                                   @Valid @RequestBody UpdatePronunciationRequest req,
                                                   Authentication auth) {
        String act = actor(auth);

        PronunciationService.UpdateRequest svcReq = new PronunciationService.UpdateRequest(
                req.speaker(),
                req.region(),
                req.audioUri(),
                req.durationMs()
        );

        try {
            Pronunciation updated = service.update(id, svcReq, act);
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

    private static PronunciationDto toDto(Pronunciation p) {
        return new PronunciationDto(
                p.getId(),
                p.getOwnerType(),
                p.getOwnerId(),
                p.getSpeaker(),
                p.getRegion(),
                p.getAudioUri(),
                p.getDurationMs()
        );
    }
}
