package com.jpablodrexler.photomanager.domain.model;

import java.util.List;

public record RenameAssetsResult(List<RenamePreview> previews, boolean applied) {}
