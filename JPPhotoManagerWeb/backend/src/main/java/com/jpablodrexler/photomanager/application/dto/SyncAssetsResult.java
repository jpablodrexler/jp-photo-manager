package com.jpablodrexler.photomanager.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SyncAssetsResult {

    private String sourceDirectory;
    private String destinationDirectory;
    private int syncedCount;
    private int deletedCount;
    private String message;
    private boolean success;

    public SyncAssetsResult(String sourceDirectory, String destinationDirectory) {
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
    }
}
