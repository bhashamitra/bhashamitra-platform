package com.bhashamitra.platform.controllers.dto;

public record UsageSentenceDto(
        String id,
        String language,
        String sentenceNative,
        String sentenceLatin,
        String translation,
        String register,
        String explanation,
        Integer difficulty,
        String status
) {}
