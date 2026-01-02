package com.bhashamitra.platform.controllers.dto;

public record MeaningDto(
        String id,
        String lemmaId,
        String meaningLanguage,
        String meaningText,
        Integer priority
) {}
