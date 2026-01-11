package com.bhashamitra.platform.controllers.dto;

import java.util.List;

public record PagedLemmasResponse(
        List<LemmaDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}