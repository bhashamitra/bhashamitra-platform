package com.bhashamitra.platform.repositories;

import com.bhashamitra.platform.models.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, String> {

    List<Language> findByEnabledTrue();
    Optional<Language> findByCodeAndEnabledTrue(String code);

    /**
     * Find language by ISO 639-1 code (mr, hi, gu, ta)
     */
    Optional<Language> findByCode(String code);
}
