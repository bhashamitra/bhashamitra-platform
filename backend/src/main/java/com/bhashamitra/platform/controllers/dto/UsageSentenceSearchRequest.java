package com.bhashamitra.platform.controllers.dto;

public record UsageSentenceSearchRequest(
        String search,
        String language,
        String status,
        int page,
        int size,
        String sort,
        String direction
) {
    public UsageSentenceSearchRequest {
        // Set defaults
        if (page < 0) page = 0;
        if (size <= 0 || size > 200) size = 20;
        if (sort == null || sort.isBlank()) sort = "sentenceNative";
        if (direction == null || !direction.equalsIgnoreCase("desc")) direction = "asc";
    }
}
