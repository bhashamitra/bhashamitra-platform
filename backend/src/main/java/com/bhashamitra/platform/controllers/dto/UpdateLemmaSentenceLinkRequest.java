package com.bhashamitra.platform.controllers.dto;

import jakarta.validation.constraints.Size;

public record UpdateLemmaSentenceLinkRequest(
        @Size(max = 36) String surfaceFormId,
        @Size(max = 20) String linkType
) {}
