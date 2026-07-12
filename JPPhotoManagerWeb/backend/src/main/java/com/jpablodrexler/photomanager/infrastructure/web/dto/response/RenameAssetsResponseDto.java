package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import java.util.List;

public record RenameAssetsResponseDto(List<RenamePreviewResponseDto> previews, boolean applied) {}
