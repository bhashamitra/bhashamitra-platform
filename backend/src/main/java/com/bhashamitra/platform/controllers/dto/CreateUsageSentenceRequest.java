package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUsageSentenceRequest(
        @NotBlank @Size(max = 10) String language,
        @NotBlank String sentenceNative,
        String sentenceLatin,
        String translation,
        @Size(max = 20) String register,
        String explanation,
        Integer difficulty
) {}

