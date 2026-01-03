package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Language;
import com.bhashamitra.platform.repositories.LanguageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
@DisplayName("LanguageService Tests")
class LanguageServiceTest {

    @Mock
    private LanguageRepository languageRepository;

    @InjectMocks
    private LanguageService languageService;

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
    @DisplayName("isLanguageEnabled - Should return true for enabled language")
    void isLanguageEnabled_ShouldReturnTrueForEnabledLanguage() {
        // Given
        when(languageRepository.findByCode("mr")).thenReturn(Optional.of(marathiLanguage));

        // When
        boolean result = languageService.isLanguageEnabled("mr");

        // Then
        assertTrue(result);
        verify(languageRepository).findByCode("mr");
    }

    @Test
    @DisplayName("isLanguageEnabled - Should return false for disabled language")
    void isLanguageEnabled_ShouldReturnFalseForDisabledLanguage() {
        // Given
        when(languageRepository.findByCode("ta")).thenReturn(Optional.of(disabledTamilLanguage));

        // When
        boolean result = languageService.isLanguageEnabled("ta");

        // Then
        assertFalse(result);
        verify(languageRepository).findByCode("ta");
    }

    @Test
    @DisplayName("isLanguageEnabled - Should return false for non-existent language")
    void isLanguageEnabled_ShouldReturnFalseForNonExistentLanguage() {
        // Given
        when(languageRepository.findByCode("xx")).thenReturn(Optional.empty());

        // When
        boolean result = languageService.isLanguageEnabled("xx");

        // Then
        assertFalse(result);
        verify(languageRepository).findByCode("xx");
    }

    @Test
    @DisplayName("getAllLanguages - Should return all languages including disabled ones")
    void getAllLanguages_ShouldReturnAllLanguagesIncludingDisabledOnes() {
        // Given
        List<Language> allLanguages = Arrays.asList(marathiLanguage, hindiLanguage, gujaratiLanguage, disabledTamilLanguage);
        when(languageRepository.findAll()).thenReturn(allLanguages);

        // When
        List<Language> result = languageService.getAllLanguages();

        // Then
        assertNotNull(result);
        assertEquals(4, result.size());
        assertTrue(result.contains(marathiLanguage));
        assertTrue(result.contains(hindiLanguage));
        assertTrue(result.contains(gujaratiLanguage));
        assertTrue(result.contains(disabledTamilLanguage));
        verify(languageRepository).findAll();
    }

    @Test
    @DisplayName("getAllLanguages - Should return empty list when no languages exist")
    void getAllLanguages_ShouldReturnEmptyListWhenNoLanguagesExist() {
        // Given
        when(languageRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<Language> result = languageService.getAllLanguages();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(languageRepository).findAll();
    }

    @Test
    @DisplayName("getEnabledLanguages - Should return only enabled languages")
    void getEnabledLanguages_ShouldReturnOnlyEnabledLanguages() {
        // Given
        List<Language> enabledLanguages = Arrays.asList(marathiLanguage, hindiLanguage, gujaratiLanguage);
        when(languageRepository.findByEnabledTrue()).thenReturn(enabledLanguages);

        // When
        List<Language> result = languageService.getEnabledLanguages();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(marathiLanguage));
        assertTrue(result.contains(hindiLanguage));
        assertTrue(result.contains(gujaratiLanguage));
        // Verify disabled language is not included
        assertFalse(result.contains(disabledTamilLanguage));
        // Verify all returned languages are enabled
        assertTrue(result.stream().allMatch(Language::getEnabled));
        verify(languageRepository).findByEnabledTrue();
    }

    @Test
    @DisplayName("getEnabledLanguages - Should return empty list when no enabled languages exist")
    void getEnabledLanguages_ShouldReturnEmptyListWhenNoEnabledLanguagesExist() {
        // Given
        when(languageRepository.findByEnabledTrue()).thenReturn(Collections.emptyList());

        // When
        List<Language> result = languageService.getEnabledLanguages();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(languageRepository).findByEnabledTrue();
    }

    @Test
    @DisplayName("getEnabledByCode - Should return enabled language by code")
    void getEnabledByCode_ShouldReturnEnabledLanguageByCode() {
        // Given
        when(languageRepository.findByCodeAndEnabledTrue("mr")).thenReturn(Optional.of(marathiLanguage));

        // When
        Language result = languageService.getEnabledByCode("mr");

        // Then
        assertNotNull(result);
        assertEquals("mr", result.getCode());
        assertEquals("Marathi", result.getName());
        assertEquals("Devanagari", result.getScript());
        assertEquals("learners-phonetic-v1", result.getTransliterationScheme());
        assertTrue(result.getEnabled());
        verify(languageRepository).findByCodeAndEnabledTrue("mr");
    }

    @Test
    @DisplayName("getEnabledByCode - Should throw exception for disabled language")
    void getEnabledByCode_ShouldThrowExceptionForDisabledLanguage() {
        // Given
        when(languageRepository.findByCodeAndEnabledTrue("ta")).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> languageService.getEnabledByCode("ta")
        );

        assertEquals(NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Enabled language not found for code: ta"));
        verify(languageRepository).findByCodeAndEnabledTrue("ta");
    }

    @Test
    @DisplayName("getEnabledByCode - Should throw exception for non-existent language")
    void getEnabledByCode_ShouldThrowExceptionForNonExistentLanguage() {
        // Given
        when(languageRepository.findByCodeAndEnabledTrue("xx")).thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> languageService.getEnabledByCode("xx")
        );

        assertEquals(NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Enabled language not found for code: xx"));
        verify(languageRepository).findByCodeAndEnabledTrue("xx");
    }

    @Test
    @DisplayName("getByCode - Should return language by code regardless of enabled status")
    void getByCode_ShouldReturnLanguageByCodeRegardlessOfEnabledStatus() {
        // Given
        when(languageRepository.findByCode("ta")).thenReturn(Optional.of(disabledTamilLanguage));

        // When
        Language result = languageService.getByCode("ta");

        // Then
        assertNotNull(result);
        assertEquals("ta", result.getCode());
        assertEquals("Tamil", result.getName());
        assertEquals("Tamil", result.getScript());
        assertEquals("ISO 15919", result.getTransliterationScheme());
        assertFalse(result.getEnabled()); // This one is disabled
        verify(languageRepository).findByCode("ta");
    }

    @Test
    @DisplayName("getByCode - Should return enabled language by code")
    void getByCode_ShouldReturnEnabledLanguageByCode() {
        // Given
        when(languageRepository.findByCode("mr")).thenReturn(Optional.of(marathiLanguage));

        // When
        Language result = languageService.getByCode("mr");

        // Then
        assertNotNull(result);
        assertEquals("mr", result.getCode());
        assertEquals("Marathi", result.getName());
        assertTrue(result.getEnabled());
        verify(languageRepository).findByCode("mr");
    }

    @Test
    @DisplayName("getByCode - Should throw IllegalArgumentException for non-existent language")
    void getByCode_ShouldThrowIllegalArgumentExceptionForNonExistentLanguage() {
        // Given
        when(languageRepository.findByCode("xx")).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> languageService.getByCode("xx")
        );

        assertEquals("Language not found for code: xx", exception.getMessage());
        verify(languageRepository).findByCode("xx");
    }

    @Test
    @DisplayName("Service methods should handle null and empty codes gracefully")
    void serviceMethodsShouldHandleNullAndEmptyCodesGracefully() {
        // Given
        when(languageRepository.findByCode(null)).thenReturn(Optional.empty());
        when(languageRepository.findByCode("")).thenReturn(Optional.empty());
        when(languageRepository.findByCodeAndEnabledTrue(null)).thenReturn(Optional.empty());
        when(languageRepository.findByCodeAndEnabledTrue("")).thenReturn(Optional.empty());

        // When & Then - isLanguageEnabled with null/empty
        assertFalse(languageService.isLanguageEnabled(null));
        assertFalse(languageService.isLanguageEnabled(""));

        // When & Then - getByCode with null/empty
        assertThrows(IllegalArgumentException.class, () -> languageService.getByCode(null));
        assertThrows(IllegalArgumentException.class, () -> languageService.getByCode(""));

        // When & Then - getEnabledByCode with null/empty
        assertThrows(ResponseStatusException.class, () -> languageService.getEnabledByCode(null));
        assertThrows(ResponseStatusException.class, () -> languageService.getEnabledByCode(""));
    }

    @Test
    @DisplayName("Service should distinguish between getByCode and getEnabledByCode behavior")
    void serviceShouldDistinguishBetweenGetByCodeAndGetEnabledByCodeBehavior() {
        // Given - Tamil exists but is disabled
        when(languageRepository.findByCode("ta")).thenReturn(Optional.of(disabledTamilLanguage));
        when(languageRepository.findByCodeAndEnabledTrue("ta")).thenReturn(Optional.empty());

        // When & Then - getByCode should return disabled language
        Language disabledResult = languageService.getByCode("ta");
        assertNotNull(disabledResult);
        assertEquals("ta", disabledResult.getCode());
        assertFalse(disabledResult.getEnabled());

        // When & Then - getEnabledByCode should throw exception for same language
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> languageService.getEnabledByCode("ta")
        );
        assertEquals(NOT_FOUND, exception.getStatusCode());

        // Verify correct repository methods were called
        verify(languageRepository).findByCode("ta");
        verify(languageRepository).findByCodeAndEnabledTrue("ta");
    }

    @Test
    @DisplayName("Service should handle repository exceptions gracefully")
    void serviceShouldHandleRepositoryExceptionsGracefully() {
        // Given
        when(languageRepository.findAll()).thenThrow(new RuntimeException("Database connection error"));
        when(languageRepository.findByEnabledTrue()).thenThrow(new RuntimeException("Database connection error"));
        when(languageRepository.findByCode("mr")).thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> languageService.getAllLanguages());
        assertThrows(RuntimeException.class, () -> languageService.getEnabledLanguages());
        assertThrows(RuntimeException.class, () -> languageService.getByCode("mr"));
        assertThrows(RuntimeException.class, () -> languageService.isLanguageEnabled("mr"));
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