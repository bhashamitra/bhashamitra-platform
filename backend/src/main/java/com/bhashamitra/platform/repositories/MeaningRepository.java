package com.bhashamitra.platform.repositories;

import com.bhashamitra.platform.models.Meaning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeaningRepository extends JpaRepository<Meaning, String> {

    // List meanings for a lemma, ordered by priority (lowest first)
    List<Meaning> findByLemma_IdOrderByPriorityAscIdAsc(String lemmaId);

    // Uniqueness helper (matches uk_meanings_lemma_lang_priority)
    boolean existsByLemma_IdAndMeaningLanguageAndPriority(String lemmaId, String meaningLanguage, Integer priority);

    // Handy lookup (optional but useful)
    Optional<Meaning> findByLemma_IdAndMeaningLanguageAndPriority(String lemmaId, String meaningLanguage, Integer priority);
}
