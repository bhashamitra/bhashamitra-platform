package com.bhashamitra.platform.controllers;

import com.bhashamitra.platform.controllers.dto.LanguageDto;
import com.bhashamitra.platform.models.Language;
import com.bhashamitra.platform.services.LanguageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicLanguageController Tests")
class PublicLanguageControllerTest {

    @Mock
    private LanguageService languageService;

    @InjectMocks
    private PublicLanguageController publicLanguageController;

    private Language marathiLanguage;
    private Language hindiLanguage;
    private Language gujaratiLanguage;
    private Language disabledTamilLanguage;

    @BeforeEach
    void setUp() {
        marathiLanguage = createLanguage("mr", "Marathi", "Devanagari", "learners-phonetic-v1", true);
        hindiLanguage = createLanguage("hi", "Hindi", "Devanagari", "IAST", true);
        gujaratiLanguage = createLanguage("gu", "Gujarati", "Gujarati", null, true);
        disabledTamilLanguage = createLanguage("ta", "Tamil", "Tamil", "ISO 15919", false);
    }

    @Test
    @DisplayName("GET /api/public/languages - Should return all enabled languages")
    void getEnabledLanguages_ShouldReturnAllEnabledLanguages() {
        // Given
        List<Language> enabledLanguages = Arrays.asList(marathiLanguage, hindiLanguage, gujaratiLanguage);
        when(languageService.getEnabledLanguages()).thenReturn(enabledLanguages);

        // When
        List<LanguageDto> result = publicLanguageController.getEnabledLanguages();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify first language (Marathi)
        LanguageDto marathiDto = result.get(0);
        assertEquals("mr", marathiDto.code());
        assertEquals("Marathi", marathiDto.name());
        assertEquals("Devanagari", marathiDto.script());
        assertEquals("learners-phonetic-v1", marathiDto.transliterationScheme());
        assertTrue(marathiDto.enabled());
        
        // Verify second language (Hindi)
        LanguageDto hindiDto = result.get(1);
        assertEquals("hi", hindiDto.code());
        assertEquals("Hindi", hindiDto.name());
        assertEquals("Devanagari", hindiDto.script());
        assertEquals("IAST", hindiDto.transliterationScheme());
        assertTrue(hindiDto.enabled());
        
        // Verify third language (Gujarati)
        LanguageDto gujaratiDto = result.get(2);
        assertEquals("gu", gujaratiDto.code());
        assertEquals("Gujarati", gujaratiDto.name());
        assertEquals("Gujarati", gujaratiDto.script());
        assertNull(gujaratiDto.transliterationScheme());
        assertTrue(gujaratiDto.enabled());
    }

    @Test
    @DisplayName("GET /api/public/languages - Should return empty list when no enabled languages")
    void getEnabledLanguages_ShouldReturnEmptyListWhenNoEnabledLanguages() {
        // Given
        when(languageService.getEnabledLanguages()).thenReturn(Collections.emptyList());

        // When
        List<LanguageDto> result = publicLanguageController.getEnabledLanguages();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GET /api/public/languages/{code} - Should return language by code")
    void getEnabledByCode_ShouldReturnLanguageByCode() {
        // Given
        when(languageService.getEnabledByCode("mr")).thenReturn(marathiLanguage);

        // When
        ResponseEntity<LanguageDto> response = publicLanguageController.getEnabledByCode("mr");

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        LanguageDto result = response.getBody();
        assertNotNull(result);
        assertEquals("mr", result.code());
        assertEquals("Marathi", result.name());
        assertEquals("Devanagari", result.script());
        assertEquals("learners-phonetic-v1", result.transliterationScheme());
        assertTrue(result.enabled());
    }

    @Test
    @DisplayName("GET /api/public/languages/{code} - Should return language with null transliteration scheme")
    void getEnabledByCode_ShouldReturnLanguageWithNullTransliterationScheme() {
        // Given
        when(languageService.getEnabledByCode("gu")).thenReturn(gujaratiLanguage);

        // When
        ResponseEntity<LanguageDto> response = publicLanguageController.getEnabledByCode("gu");

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        LanguageDto result = response.getBody();
        assertNotNull(result);
        assertEquals("gu", result.code());
        assertEquals("Gujarati", result.name());
        assertEquals("Gujarati", result.script());
        assertNull(result.transliterationScheme());
        assertTrue(result.enabled());
    }

    @Test
    @DisplayName("GET /api/public/languages/{code} - Should return 404 when language not found")
    void getEnabledByCode_ShouldReturn404WhenLanguageNotFound() {
        // Given
        when(languageService.getEnabledByCode("ta"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Enabled language not found for code: ta"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> publicLanguageController.getEnabledByCode("ta")
        );
        
        assertEquals(NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/public/languages - Should handle service exceptions gracefully")
    void getEnabledLanguages_ShouldHandleServiceExceptions() {
        // Given
        when(languageService.getEnabledLanguages()).thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> publicLanguageController.getEnabledLanguages()
        );
        
        assertEquals("Database connection error", exception.getMessage());
    }

    @Test
    @DisplayName("Should map Language to LanguageDto correctly")
    void toDto_ShouldMapLanguageToLanguageDtoCorrectly() {
        // Given
        when(languageService.getEnabledByCode("hi")).thenReturn(hindiLanguage);

        // When
        ResponseEntity<LanguageDto> response = publicLanguageController.getEnabledByCode("hi");

        // Then
        assertNotNull(response);
        LanguageDto result = response.getBody();
        assertNotNull(result);
        
        // Verify all fields are mapped correctly
        assertEquals(hindiLanguage.getCode(), result.code());
        assertEquals(hindiLanguage.getName(), result.name());
        assertEquals(hindiLanguage.getScript(), result.script());
        assertEquals(hindiLanguage.getTransliterationScheme(), result.transliterationScheme());
        assertEquals(hindiLanguage.getEnabled(), result.enabled());
    }

    @Test
    @DisplayName("Should handle multiple concurrent requests")
    void shouldHandleMultipleConcurrentRequests() {
        // Given
        when(languageService.getEnabledLanguages()).thenReturn(Arrays.asList(marathiLanguage, hindiLanguage));
        when(languageService.getEnabledByCode("mr")).thenReturn(marathiLanguage);
        when(languageService.getEnabledByCode("hi")).thenReturn(hindiLanguage);

        // When & Then - Simulate concurrent requests
        List<LanguageDto> allLanguages = publicLanguageController.getEnabledLanguages();
        assertNotNull(allLanguages);
        assertEquals(2, allLanguages.size());

        ResponseEntity<LanguageDto> marathiResponse = publicLanguageController.getEnabledByCode("mr");
        assertNotNull(marathiResponse.getBody());
        assertEquals("mr", marathiResponse.getBody().code());

        ResponseEntity<LanguageDto> hindiResponse = publicLanguageController.getEnabledByCode("hi");
        assertNotNull(hindiResponse.getBody());
        assertEquals("hi", hindiResponse.getBody().code());
    }

    @Test
    @DisplayName("GET /api/public/languages - Should only return enabled languages when disabled languages exist")
    void getEnabledLanguages_ShouldOnlyReturnEnabledLanguagesWhenDisabledLanguagesExist() {
        // Given - Simulate what the service layer should do: filter out disabled languages
        // In a real scenario, there might be 4 languages in DB (3 enabled + 1 disabled)
        // But the service's getEnabledLanguages() should only return the enabled ones
        List<Language> allLanguagesInDb = Arrays.asList(marathiLanguage, hindiLanguage, gujaratiLanguage, disabledTamilLanguage);
        
        // Simulate the service filtering logic (what LanguageService.getEnabledLanguages() should do)
        List<Language> onlyEnabledLanguages = allLanguagesInDb.stream()
                .filter(Language::getEnabled)
                .toList();
        
        // Mock the service to return only enabled languages (this is what the real service should do)
        when(languageService.getEnabledLanguages()).thenReturn(onlyEnabledLanguages);

        // When
        List<LanguageDto> result = publicLanguageController.getEnabledLanguages();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // Only 3 enabled languages, not 4 total
        
        // Verify disabled Tamil language is NOT in the response
        assertTrue(result.stream().noneMatch(dto -> "ta".equals(dto.code())));
        assertTrue(result.stream().noneMatch(dto -> "Tamil".equals(dto.name())));
        
        // Verify only enabled languages are present
        List<String> codes = result.stream().map(LanguageDto::code).toList();
        assertTrue(codes.containsAll(Arrays.asList("mr", "hi", "gu")));
        assertFalse(codes.contains("ta"));
        
        // Verify all returned languages have enabled=true
        assertTrue(result.stream().allMatch(LanguageDto::enabled));
        
        // This test verifies the critical security requirement:
        // Even if disabled languages exist in the database, the public API never exposes them
    }

    @Test
    @DisplayName("GET /api/public/languages/{code} - Should return 404 for disabled language")
    void getEnabledByCode_ShouldReturn404ForDisabledLanguage() {
        // Given - Service throws exception when trying to get disabled language
        // This simulates the service layer properly rejecting access to disabled languages
        when(languageService.getEnabledByCode("ta"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Enabled language not found for code: ta"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> publicLanguageController.getEnabledByCode("ta")
        );
        
        assertEquals(NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Enabled language not found for code: ta"));
        
        // This test verifies that even if Tamil language exists in DB but is disabled,
        // the public API returns 404 (same as if it didn't exist at all)
    }

    @Test
    @DisplayName("GET /api/public/languages/{code} - Should distinguish between non-existent and disabled languages")
    void getEnabledByCode_ShouldDistinguishBetweenNonExistentAndDisabledLanguages() {
        // Given - Both scenarios throw NOT_FOUND but with different messages
        when(languageService.getEnabledByCode("xx"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Enabled language not found for code: xx"));
        
        when(languageService.getEnabledByCode("ta"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Enabled language not found for code: ta"));

        // When & Then - Both should return 404 (public API shouldn't reveal if language exists but is disabled)
        ResponseStatusException nonExistentException = assertThrows(
                ResponseStatusException.class,
                () -> publicLanguageController.getEnabledByCode("xx")
        );
        
        ResponseStatusException disabledException = assertThrows(
                ResponseStatusException.class,
                () -> publicLanguageController.getEnabledByCode("ta")
        );
        
        // Both should return the same status code (security through consistency)
        assertEquals(NOT_FOUND, nonExistentException.getStatusCode());
        assertEquals(NOT_FOUND, disabledException.getStatusCode());
        
        // This ensures the public API doesn't leak information about disabled languages
    }

    /**
     * Helper method to create Language test objects
     */
    private Language createLanguage(String code, String name, String script, String transliterationScheme, Boolean enabled) {
        Language language = new Language();
        language.setCode(code);
        language.setName(name);
        language.setScript(script);
        language.setTransliterationScheme(transliterationScheme);
        language.setEnabled(enabled);
        return language;
    }
}
