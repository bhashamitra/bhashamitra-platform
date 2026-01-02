package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.Size;

public record UpdateLemmaRequest(
        @Size(max = 255) String lemmaNative,
        @Size(max = 255) String lemmaLatin,
        @Size(max = 50) String pos,
        String notes,
        @Size(max = 20) String status
) {}
