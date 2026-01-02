package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.LanguageDto;
import com.bhashamitra.platform.models.Language;
import com.bhashamitra.platform.services.LanguageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/languages")
public class PublicLanguageController {

    private final LanguageService languageService;

    public PublicLanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping
    public List<LanguageDto> getEnabledLanguages() {
        return languageService.getEnabledLanguages().stream()
                .map(PublicLanguageController::toDto)
                .toList();
    }

    @GetMapping("/{code}")
    public ResponseEntity<LanguageDto> getEnabledByCode(@PathVariable String code) {
        return ResponseEntity.ok(toDto(languageService.getEnabledByCode(code)));
    }


    private static LanguageDto toDto(Language l) {
        return new LanguageDto(
                l.getCode(),
                l.getName(),
                l.getScript(),
                l.getTransliterationScheme(),
                l.getEnabled()
        );
    }
}
