package com.jpablodrexler.photomanager.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadRequest(@NotBlank String folderPath) {
}
