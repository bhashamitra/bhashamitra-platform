package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.PronunciationDto;
import com.bhashamitra.platform.models.Pronunciation;
import com.bhashamitra.platform.services.PronunciationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/pronunciations")
public class PublicPronunciationController {

    private final PronunciationService service;

    public PublicPronunciationController(PronunciationService service) {
        this.service = service;
    }

    /**
     * Public pronunciations for an owner.
     * Guardrails:
     * - ownerType=LEMMA => lemma must be PUBLISHED
     * - ownerType=SENTENCE => sentence must be PUBLISHED
     */
    @GetMapping
    public ResponseEntity<List<PronunciationDto>> listPublicByOwner(
            @RequestParam String ownerType,
            @RequestParam String ownerId
    ) {
        try {
            List<Pronunciation> out = service.listPublicByOwner(ownerType, ownerId);
            return ResponseEntity.ok(out.stream().map(PublicPronunciationController::toDto).toList());
        } catch (IllegalArgumentException e) {
            // includes "not published/not found" cases from your service calls
            return ResponseEntity.badRequest().build();
        }
    }

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
