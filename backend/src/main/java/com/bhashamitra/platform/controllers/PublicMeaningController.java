package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.MeaningDto;
import com.bhashamitra.platform.models.Meaning;
import com.bhashamitra.platform.services.LemmaService;
import com.bhashamitra.platform.services.MeaningService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/meanings")
public class PublicMeaningController {

    private final MeaningService meaningService;
    private final LemmaService lemmaService;

    public PublicMeaningController(MeaningService meaningService, LemmaService lemmaService) {
        this.meaningService = meaningService;
        this.lemmaService = lemmaService;
    }

    /**
     * Public meanings for a lemma.
     * Guardrail: lemma must be PUBLISHED.
     */
    @GetMapping
    public List<MeaningDto> listForPublishedLemma(@RequestParam String lemmaId) {
        // throws if not published / not found
        lemmaService.getPublishedById(lemmaId);

        return meaningService.listByLemmaId(lemmaId).stream()
                .map(PublicMeaningController::toDto)
                .toList();
    }

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
