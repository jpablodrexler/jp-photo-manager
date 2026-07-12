package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

public record RenamePreviewResponseDto(Long assetId, String oldName, String newName) {}
