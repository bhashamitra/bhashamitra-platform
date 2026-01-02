package com.bhashamitra.platform.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "languages")
@Data
@EqualsAndHashCode(callSuper = true)
public class Language extends Auditable {

    /**
     * ISO 639-1 language code (mr, hi, gu, ta, etc.)
     */
    @Column(name = "code", length = 10, nullable = false, unique = true)
    private String code;

    /**
     * Human-readable language name
     */
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    /**
     * Script used by the language (Devanagari, Gujarati, Tamil, etc.)
     */
    @Column(name = "script", length = 50, nullable = false)
    private String script;

    /**
     * Optional transliteration scheme (IAST, learners-phonetic-v1, etc.)
     */
    @Column(name = "transliteration_scheme", length = 50)
    private String transliterationScheme;

    /**
     * Whether this language is enabled in the platform
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = Boolean.TRUE;
}
