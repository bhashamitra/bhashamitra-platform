package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.CreateSurfaceFormRequest;
import com.bhashamitra.platform.controllers.dto.SurfaceFormDto;
import com.bhashamitra.platform.controllers.dto.UpdateSurfaceFormRequest;
import com.bhashamitra.platform.models.SurfaceForm;
import com.bhashamitra.platform.services.SurfaceFormService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bhashamitra.platform.security.ActorUtil.actor;

@RestController
@RequestMapping("/api/admin/surface-forms")
public class AdminSurfaceFormController {

    private final SurfaceFormService surfaceFormService;

    public AdminSurfaceFormController(SurfaceFormService surfaceFormService) {
        this.surfaceFormService = surfaceFormService;
    }

    // --------------------
    // READ
    // --------------------

    @GetMapping("/{id}")
    public ResponseEntity<SurfaceFormDto> getById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(toDto(surfaceFormService.getById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public List<SurfaceFormDto> listByLemmaId(@RequestParam String lemmaId) {
        return surfaceFormService.listByLemmaId(lemmaId).stream()
                .map(AdminSurfaceFormController::toDto)
                .toList();
    }

    // --------------------
    // CREATE
    // --------------------

    @PostMapping
    public ResponseEntity<SurfaceFormDto> create(@Valid @RequestBody CreateSurfaceFormRequest req,
                                                 Authentication auth) {
        String act = actor(auth);

        SurfaceFormService.SurfaceFormCreateRequest svcReq =
                new SurfaceFormService.SurfaceFormCreateRequest(
                        req.lemmaId(),
                        req.formNative(),
                        req.formLatin(),
                        req.formType(),
                        req.notes()
                );

        try {
            SurfaceForm created = surfaceFormService.create(svcReq, act);
            return ResponseEntity.ok(toDto(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --------------------
    // UPDATE
    // --------------------

    @PutMapping("/{id}")
    public ResponseEntity<SurfaceFormDto> update(@PathVariable String id,
                                                 @Valid @RequestBody UpdateSurfaceFormRequest req,
                                                 Authentication auth) {
        String act = actor(auth);

        SurfaceFormService.SurfaceFormUpdateRequest svcReq =
                new SurfaceFormService.SurfaceFormUpdateRequest(
                        req.formNative(),
                        req.formLatin(),
                        req.formType(),
                        req.notes()
                );

        try {
            SurfaceForm updated = surfaceFormService.update(id, svcReq, act);
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
            surfaceFormService.delete(id, actor(auth));
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --------------------
    // Helpers
    // --------------------

    private static SurfaceFormDto toDto(SurfaceForm sf) {
        return new SurfaceFormDto(
                sf.getId(),
                sf.getLemma() != null ? sf.getLemma().getId() : null,
                sf.getFormNative(),
                sf.getFormLatin(),
                sf.getFormType(),
                sf.getNotes()
        );
    }
}
