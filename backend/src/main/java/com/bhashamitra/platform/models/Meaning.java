package com.bhashamitra.platform.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "meanings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_meanings_lemma_lang_priority",
                        columnNames = {"lemma_id", "meaning_language", "priority"}
                )
        },
        indexes = {
                @Index(name = "idx_meanings_lemma", columnList = "lemma_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
public class Meaning extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "meaning_language", length = 10, nullable = false)
    private String meaningLanguage;

    @Column(name = "meaning_text", length = 1024, nullable = false)
    private String meaningText;

    @Column(name = "priority", nullable = false)
    private Integer priority = 1;
}
