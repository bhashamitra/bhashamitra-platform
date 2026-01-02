package com.bhashamitra.platform.repositories;

import com.bhashamitra.platform.models.Lemma;
import com.bhashamitra.platform.models.LemmaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends JpaRepository<Lemma, String> {

    // -------- Admin/editor queries (all statuses) --------

    List<Lemma> findByLanguageOrderByLemmaNativeAsc(String language);

    List<Lemma> findByLanguageAndStatusOrderByLemmaNativeAsc(String language, LemmaStatus status);

    Optional<Lemma> findByLanguageAndLemmaNative(String language, String lemmaNative);

    boolean existsByLanguageAndLemmaNative(String language, String lemmaNative);

    // -------- Public queries (only published) --------

    Optional<Lemma> findByIdAndStatus(String id, LemmaStatus status);
}
