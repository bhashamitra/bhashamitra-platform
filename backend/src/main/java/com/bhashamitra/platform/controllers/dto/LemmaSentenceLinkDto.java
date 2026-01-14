package com.bhashamitra.platform.controllers.dto;

public record LemmaSentenceLinkDto(
        String id,
        String lemmaId,
        String sentenceId,
        String meaningId,
        String surfaceFormId,
        String linkType
) {}
