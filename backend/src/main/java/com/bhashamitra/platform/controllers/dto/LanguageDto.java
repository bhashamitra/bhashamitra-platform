package com.bhashamitra.platform.controllers.dto;

public record LanguageDto(
        String code,
        String name,
        String script,
        String transliterationScheme,
        Boolean enabled
) {}
