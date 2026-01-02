package com.bhashamitra.platform.repositories;

import com.bhashamitra.platform.models.LemmaSentenceLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LemmaSentenceLinkRepository extends JpaRepository<LemmaSentenceLink, String> {

    // Uniqueness helper (matches uk_lsl_lemma_sentence)
    boolean existsByLemma_IdAndSentence_Id(String lemmaId, String sentenceId);

    Optional<LemmaSentenceLink> findByLemma_IdAndSentence_Id(String lemmaId, String sentenceId);

    // Browse links by lemma (useful for "show me examples for this word")
    List<LemmaSentenceLink> findByLemma_IdOrderByCreatedDateDescIdDesc(String lemmaId);

    // Browse links by sentence (useful for "which lemmas are in this sentence?")
    List<LemmaSentenceLink> findBySentence_IdOrderByCreatedDateDescIdDesc(String sentenceId);
}
