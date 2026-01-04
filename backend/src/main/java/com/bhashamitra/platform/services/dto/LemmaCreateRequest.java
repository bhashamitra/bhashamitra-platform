package com.bhashamitra.platform.services.dto;

import com.bhashamitra.platform.models.LemmaStatus;

public record LemmaCreateRequest(
        String language,
        String lemmaNative,
        String lemmaLatin,
        String pos,
        String notes,
        LemmaStatus status
) {}