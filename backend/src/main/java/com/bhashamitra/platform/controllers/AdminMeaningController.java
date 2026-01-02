package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.CreateMeaningRequest;
import com.bhashamitra.platform.controllers.dto.MeaningDto;
import com.bhashamitra.platform.controllers.dto.UpdateMeaningRequest;
import com.bhashamitra.platform.models.Meaning;
import com.bhashamitra.platform.services.MeaningService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bhashamitra.platform.security.ActorUtil.actor;

@RestController
@RequestMapping("/api/admin/meanings")
public class AdminMeaningController {

    private final MeaningService meaningService;

    public AdminMeaningController(MeaningService meaningService) {
        this.meaningService = meaningService;
    }

    // --------------------
    // READ
    // --------------------

    @GetMapping("/{id}")
    public ResponseEntity<MeaningDto> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(toDto(meaningService.getById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public List<MeaningDto> listByLemmaId(@RequestParam String lemmaId) {
        return meaningService.listByLemmaId(lemmaId).stream()
                .map(AdminMeaningController::toDto)
                .toList();
    }

    // --------------------
    // CREATE
    // --------------------

    @PostMapping
    public ResponseEntity<MeaningDto> create(@Valid @RequestBody CreateMeaningRequest req, Authentication auth) {
        String actor = actor(auth);

        MeaningService.MeaningCreateRequest svcReq = new MeaningService.MeaningCreateRequest(
                req.lemmaId(),
                req.meaningLanguage(),
                req.meaningText(),
                req.priority()
        );

        try {
            Meaning created = meaningService.create(svcReq, actor);
            return ResponseEntity.ok(toDto(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // UPDATE
    // --------------------

    @PutMapping("/{id}")
    public ResponseEntity<MeaningDto> update(@PathVariable String id,
                                             @Valid @RequestBody UpdateMeaningRequest req,
                                             Authentication auth) {
        String actor = actor(auth);

        MeaningService.MeaningUpdateRequest svcReq = new MeaningService.MeaningUpdateRequest(
                req.meaningLanguage(),
                req.meaningText(),
                req.priority()
        );

        try {
            Meaning updated = meaningService.update(id, svcReq, actor);
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
        meaningService.delete(id, actor(auth));
        return ResponseEntity.noContent().build();
    }


    // --------------------
    // Helpers
    // --------------------

    private static MeaningDto toDto(Meaning m) {
        return new MeaningDto(
                m.getId(),
                m.getLemma() != null ? m.getLemma().getId() : null,
                m.getMeaningLanguage(),
                m.getMeaningText(),
                m.getPriority()
        );
    }
}
