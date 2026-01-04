package com.bhashamitra.platform.services.dto;

public record LemmaUpdateRequest(
        String language,
        String lemmaNative,
        String lemmaLatin,
        String pos,
        String notes
) {}