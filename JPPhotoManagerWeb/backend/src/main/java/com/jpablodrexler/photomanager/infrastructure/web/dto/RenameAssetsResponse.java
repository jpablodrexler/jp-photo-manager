package com.jpablodrexler.photomanager.infrastructure.web.dto;

import java.util.List;

public record RenameAssetsResponse(List<RenamePreviewDto> previews, boolean applied) {}
