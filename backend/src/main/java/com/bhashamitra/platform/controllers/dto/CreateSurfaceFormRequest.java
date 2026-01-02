package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSurfaceFormRequest(
        @NotBlank @Size(max = 36) String lemmaId,
        @NotBlank @Size(max = 255) String formNative,
        @Size(max = 255) String formLatin,
        @Size(max = 50) String formType,
        String notes
) {}
