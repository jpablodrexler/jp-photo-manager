package com.jpablodrexler.photomanager.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConvertAssetsResult {

    private String sourceDirectory;
    private String destinationDirectory;
    private int convertedCount;
    private int failedCount;
    private String message;
    private boolean success;

    public ConvertAssetsResult(String sourceDirectory, String destinationDirectory) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
    }
}
