package com.bhashamitra.platform.controllers.dto;

public record LemmaDto(
        String id,
        String language,
        String lemmaNative,
        String lemmaLatin,
        String pos,
        String notes,
        String status
) {}
