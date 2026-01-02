package com.bhashamitra.platform.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "usage_sentences",
        indexes = {
                @Index(name = "idx_usage_sentences_language", columnList = "language"),
                @Index(name = "idx_usage_sentences_status", columnList = "status")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
public class UsageSentence extends Auditable {

    /**
     * ISO 639-1 code (mr, hi, gu, ta) referencing languages.code
     */
    @Column(name = "language", length = 10, nullable = false)
    private String language;

    @Column(name = "sentence_native", columnDefinition = "TEXT", nullable = false)
    private String sentenceNative;

    @Column(name = "sentence_latin", columnDefinition = "TEXT")
    private String sentenceLatin;

    @Column(name = "translation", columnDefinition = "TEXT")
    private String translation;

    /**
     * spoken, neutral, formal (stored as-is; DB default is "neutral")
     */
    @Column(name = "register", length = 20, nullable = false)
    private String register = "neutral";

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "difficulty")
    private Integer difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UsageSentenceStatus status = UsageSentenceStatus.DRAFT;
}
