package com.bhashamitra.platform.controllers.dto;

public record PronunciationDto(
        String id,
        String ownerType,
        String ownerId,
        String speaker,
        String region,
        String audioUri,
        Integer durationMs
) {}
