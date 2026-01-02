package com.bhashamitra.platform.repositories;

import com.bhashamitra.platform.models.Pronunciation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PronunciationRepository extends JpaRepository<Pronunciation, String> {

    List<Pronunciation> findByOwnerTypeAndOwnerIdOrderByCreatedDateDescIdDesc(String ownerType, String ownerId);

    boolean existsByOwnerTypeAndOwnerIdAndAudioUri(String ownerType, String ownerId, String audioUri);
}
