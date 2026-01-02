package com.bhashamitra.platform.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "pronunciations",
        indexes = {
                @Index(name = "idx_pron_owner", columnList = "owner_type, owner_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
public class Pronunciation extends Auditable {

    /**
     * e.g. LEMMA, SENTENCE (keep flexible; stored as VARCHAR in DB)
     */
    @Column(name = "owner_type", length = 20, nullable = false)
    private String ownerType;

    /**
     * UUID of lemma / sentence / surface form / etc
     */
    @Column(name = "owner_id", length = 36, nullable = false)
    private String ownerId;

    @Column(name = "speaker", length = 100)
    private String speaker;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "audio_uri", length = 1024, nullable = false)
    private String audioUri;

    @Column(name = "duration_ms")
    private Integer durationMs;
}
