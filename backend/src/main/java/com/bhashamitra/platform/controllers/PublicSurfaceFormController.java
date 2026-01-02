// ===== FILE: ./com/bhashamitra/platform/controllers/PublicSurfaceFormController.java =====
package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.SurfaceFormDto;
import com.bhashamitra.platform.models.SurfaceForm;
import com.bhashamitra.platform.services.LemmaService;
import com.bhashamitra.platform.services.SurfaceFormService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/surface-forms")
public class PublicSurfaceFormController {

    private final SurfaceFormService surfaceFormService;
    private final LemmaService lemmaService;

    public PublicSurfaceFormController(SurfaceFormService surfaceFormService, LemmaService lemmaService) {
        this.surfaceFormService = surfaceFormService;
        this.lemmaService = lemmaService;
    }

    /**
     * Public surface forms for a lemma.
     * Guardrail: lemma must be PUBLISHED.
     */
    @GetMapping
    public List<SurfaceFormDto> listForPublishedLemma(@RequestParam String lemmaId) {
        lemmaService.getPublishedById(lemmaId);

        return surfaceFormService.listByLemmaId(lemmaId).stream()
                .map(PublicSurfaceFormController::toDto)
                .toList();
    }

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
