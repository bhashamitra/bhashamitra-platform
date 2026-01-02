package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMeaningRequest(
        @NotBlank @Size(max = 36) String lemmaId,
        @NotBlank @Size(max = 10) String meaningLanguage,
        @NotBlank @Size(max = 1024) String meaningText,
        @NotNull Integer priority
) {}
