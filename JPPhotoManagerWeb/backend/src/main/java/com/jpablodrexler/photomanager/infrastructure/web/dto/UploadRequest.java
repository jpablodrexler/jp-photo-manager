package com.jpablodrexler.photomanager.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadRequest(@NotBlank String folderPath) {
}
