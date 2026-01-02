package com.bhashamitra.platform.controllers.dto;

public record LemmaSentenceLinkDto(
        String id,
        String lemmaId,
        String sentenceId,
        String surfaceFormId,
        String linkType
) {}
