package com.bhashamitra.platform.repositories;

import com.bhashamitra.platform.models.UsageSentence;
import com.bhashamitra.platform.models.UsageSentenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface UsageSentenceRepository extends JpaRepository<UsageSentence, String>, JpaSpecificationExecutor<UsageSentence> {

    // -------- Admin/editor queries (all statuses) --------

    List<UsageSentence> findByLanguageOrderBySentenceNativeAsc(String language);

    List<UsageSentence> findByLanguageAndStatusOrderBySentenceNativeAsc(String language, UsageSentenceStatus status);
}
