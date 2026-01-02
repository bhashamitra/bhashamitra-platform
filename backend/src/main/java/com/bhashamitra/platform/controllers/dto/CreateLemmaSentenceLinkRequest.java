package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLemmaSentenceLinkRequest(
        @NotBlank @Size(max = 36) String lemmaId,
        @NotBlank @Size(max = 36) String sentenceId,
        @Size(max = 36) String surfaceFormId,
        @Size(max = 20) String linkType
) {}
