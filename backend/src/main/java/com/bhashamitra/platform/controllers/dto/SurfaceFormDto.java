package com.bhashamitra.platform.controllers.dto;

public record SurfaceFormDto(
        String id,
        String lemmaId,
        String formNative,
        String formLatin,
        String formType,
        String notes
) {}
