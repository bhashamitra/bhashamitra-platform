package com.bhashamitra.platform.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "lemmas")
@Data
@EqualsAndHashCode(callSuper = true)
public class Lemma extends Auditable {

    /**
     * ISO 639-1 code (mr, hi, gu, ta) referencing languages.code
     * Column name is 'language' in DB.
     */
    @Column(name = "language", length = 10, nullable = false)
    private String language;

    @Column(name = "lemma_native", length = 255, nullable = false)
    private String lemmaNative;

    @Column(name = "lemma_latin", length = 255)
    private String lemmaLatin;

    @Column(name = "pos", length = 50)
    private String pos;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private LemmaStatus status = LemmaStatus.DRAFT;
}
