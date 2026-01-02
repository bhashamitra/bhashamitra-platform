package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.Size;

public record UpdateUsageSentenceRequest(
        @Size(max = 10) String language,
        String sentenceNative,
        String sentenceLatin,
        String translation,
        @Size(max = 20) String register,
        String explanation,
        Integer difficulty,
        @Size(max = 20) String status
) {}
