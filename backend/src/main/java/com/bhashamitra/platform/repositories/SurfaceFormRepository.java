package com.bhashamitra.platform.repositories;

import com.bhashamitra.platform.models.SurfaceForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SurfaceFormRepository extends JpaRepository<SurfaceForm, String> {

    // Uniqueness helper (matches uk_surface_forms_lemma_form_native)
    boolean existsByLemma_IdAndFormNative(String lemmaId, String formNative);

    Optional<SurfaceForm> findByLemma_IdAndFormNative(String lemmaId, String formNative);

    // Browse forms for a lemma
    List<SurfaceForm> findByLemma_IdOrderByFormNativeAscIdAsc(String lemmaId);
}
