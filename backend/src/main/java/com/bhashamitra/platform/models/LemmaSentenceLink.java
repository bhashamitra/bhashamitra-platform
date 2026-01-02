package com.bhashamitra.platform.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "lemma_sentence_links",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_lsl_lemma_sentence",
                        columnNames = {"lemma_id", "sentence_id"}
                )
        },
        indexes = {
                @Index(name = "idx_lsl_lemma", columnList = "lemma_id"),
                @Index(name = "idx_lsl_sentence", columnList = "sentence_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
public class LemmaSentenceLink extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sentence_id", nullable = false)
    private UsageSentence sentence;

    /**
     * Optional: points to a specific surface form of the lemma used in the sentence.
     * FK in DB is ON DELETE SET NULL.
     * NOTE: This entity is defined later (SurfaceForm). We'll create that model next.
     */
    @Column(name = "surface_form_id", length = 36)
    private String surfaceFormId;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", length = 20, nullable = false)
    private LemmaSentenceLinkType linkType = LemmaSentenceLinkType.EXACT;
}
