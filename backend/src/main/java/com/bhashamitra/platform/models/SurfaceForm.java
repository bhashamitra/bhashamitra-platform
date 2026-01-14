package com.bhashamitra.platform.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(
        name = "surface_forms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_surface_forms_lemma_form_native",
                        columnNames = {"lemma_id", "form_native"}
                )
        },
        indexes = {
                @Index(name = "idx_surface_forms_lemma", columnList = "lemma_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
public class SurfaceForm extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "form_native", length = 255, nullable = false)
    private String formNative;

    @Column(name = "form_latin", length = 255)
    private String formLatin;

    /**
     * Form type: infinitive, finite_verb_present, finite_verb_past, etc.
     * Keep as String for flexibility (no enum needed yet).
     * Required field - NOT NULL at database level.
     */
    @Column(name = "form_type", length = 50, nullable = false)
    private String formType;

    /**
     * Morphological features stored as JSON (e.g., gender, number, person, politeness).
     * Stored as TEXT in database, serialized/deserialized as JSON string.
     */
    @Column(name = "features_json", columnDefinition = "json")
    private String featuresJson;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
