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
     * plural, oblique, informal, alt_spelling, spoken, etc.
     * Keep as String for flexibility (no enum needed yet).
     */
    @Column(name = "form_type", length = 50)
    private String formType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
