package com.bhashamitra.platform.controllers.dto;

public record LemmaSearchRequest(
        String search,
        String language,
        String status,
        String pos,
        int page,
        int size,
        String sort,
        String direction
) {
    public LemmaSearchRequest {
        // Set defaults
        if (page < 0) page = 0;
        if (size <= 0 || size > 200) size = 20;
        if (sort == null || sort.isBlank()) sort = "lemmaNative";
        if (direction == null || !direction.equalsIgnoreCase("desc")) direction = "asc";
    }
}