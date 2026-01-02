package com.bhashamitra.platform.services;

import com.bhashamitra.platform.models.Language;
import com.bhashamitra.platform.repositories.LanguageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LanguageService {

    private final LanguageRepository languageRepository;

    public LanguageService(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    public boolean isLanguageEnabled(String code) {
        return languageRepository.findByCode(code)
                .map(Language::getEnabled)
                .orElse(false);
    }


    /**
     * Return all languages (enabled + disabled).
     * Used by admin/editor UI.
     */
    public List<Language> getAllLanguages() {
        return languageRepository.findAll();
    }

    /**
     * Return only enabled languages.
     * Used by editor dropdowns and public UI later.
     */

    public List<Language> getEnabledLanguages() {
        return languageRepository.findByEnabledTrue();
    }

    public Language getEnabledByCode(String code) {
        return languageRepository.findByCodeAndEnabledTrue(code)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "Enabled language not found for code: " + code
                ));
    }


    /**
     * Fetch a language by ISO 639-1 code.
     */
    public Language getByCode(String code) {
        return languageRepository.findByCode(code)
                .orElseThrow(() ->
                        new IllegalArgumentException("Language not found for code: " + code)
                );
    }
}
