package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLemmaRequest(
        @NotBlank @Size(max = 10) String language,
        @NotBlank @Size(max = 255) String lemmaNative,
        @Size(max = 255) String lemmaLatin,
        @Size(max = 50) String pos,
        String notes
) {}
