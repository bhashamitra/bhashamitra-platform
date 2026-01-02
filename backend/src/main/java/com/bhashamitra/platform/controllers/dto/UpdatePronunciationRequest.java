package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.Size;

public record UpdatePronunciationRequest(
        @Size(max = 100) String speaker,
        @Size(max = 100) String region,
        @Size(max = 1024) String audioUri,
        Integer durationMs
) {}
