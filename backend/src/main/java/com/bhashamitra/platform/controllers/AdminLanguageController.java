package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.LanguageDto;
import com.bhashamitra.platform.models.Language;
import com.bhashamitra.platform.services.LanguageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/languages")
public class AdminLanguageController {

    private final LanguageService languageService;

    public AdminLanguageController(LanguageService languageService) {
        this.languageService = languageService;
    }

    @GetMapping
    public List<LanguageDto> getAllLanguages() {
        return languageService.getAllLanguages().stream()
                .map(AdminLanguageController::toDto)
                .toList();
    }

    @GetMapping("/{code}")
    public ResponseEntity<LanguageDto> getByCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(toDto(languageService.getByCode(code)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{code}/enabled")
    public boolean isEnabled(@PathVariable String code) {
        return languageService.isLanguageEnabled(code);
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
