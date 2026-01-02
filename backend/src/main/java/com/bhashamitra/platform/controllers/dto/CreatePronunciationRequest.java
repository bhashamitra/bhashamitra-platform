package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePronunciationRequest(
        @NotBlank @Size(max = 20) String ownerType,
        @NotBlank @Size(max = 36) String ownerId,
        @Size(max = 100) String speaker,
        @Size(max = 100) String region,
        @NotBlank @Size(max = 1024) String audioUri,
        Integer durationMs
) {}
