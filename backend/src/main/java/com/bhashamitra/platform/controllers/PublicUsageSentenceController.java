package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.UsageSentenceDto;
import com.bhashamitra.platform.models.UsageSentence;
import com.bhashamitra.platform.services.UsageSentenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/sentences")
public class PublicUsageSentenceController {

    private final UsageSentenceService usageSentenceService;

    public PublicUsageSentenceController(UsageSentenceService usageSentenceService) {
        this.usageSentenceService = usageSentenceService;
    }

    // List published sentences by language
    @GetMapping
    public List<UsageSentenceDto> listPublishedByLanguage(@RequestParam String language) {
        return usageSentenceService.listPublishedByLanguage(language).stream()
                .map(PublicUsageSentenceController::toDto)
                .toList();
    }

    // Get a published sentence by id
    @GetMapping("/{id}")
    public ResponseEntity<UsageSentenceDto> getPublishedById(@PathVariable String id) {
        try {
            return ResponseEntity.ok(toDto(usageSentenceService.getPublishedById(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

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
