package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.Size;

public record UpdateMeaningRequest(
        @Size(max = 10) String meaningLanguage,
        @Size(max = 1024) String meaningText,
        Integer priority
) {}
