package com.jpablodrexler.photomanager.application.dto;

import java.util.List;

public record RenameAssetsResult(List<RenamePreview> previews, boolean applied) {}
