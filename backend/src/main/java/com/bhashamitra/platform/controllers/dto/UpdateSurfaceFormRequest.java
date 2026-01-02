package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.Size;

public record UpdateSurfaceFormRequest(
        @Size(max = 255) String formNative,
        @Size(max = 255) String formLatin,
        @Size(max = 50) String formType,
        String notes
) {}
