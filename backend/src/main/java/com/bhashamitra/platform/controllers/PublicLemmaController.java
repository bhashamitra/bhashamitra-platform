package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.LemmaDto;
import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.services.LemmaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/lemmas")
public class PublicLemmaController {

    private final LemmaService lemmaService;

    public PublicLemmaController(LemmaService lemmaService) {
        this.lemmaService = lemmaService;
    }

    // List published lemmas by language
    @GetMapping
    public List<LemmaDto> listPublishedByLanguage(@RequestParam String language) {
        return lemmaService.listPublishedByLanguage(language).stream()
                .map(PublicLemmaController::toDto)
                .toList();
    }

    // Get a published lemma by id
    @GetMapping("/{id}")
    public ResponseEntity<LemmaDto> getPublishedById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(toDto(lemmaService.getPublishedById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

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
