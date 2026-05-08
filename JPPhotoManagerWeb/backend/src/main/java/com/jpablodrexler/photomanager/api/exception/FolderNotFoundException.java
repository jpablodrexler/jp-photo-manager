package com.jpablodrexler.photomanager.api.exception;

public class FolderNotFoundException extends RuntimeException {
    public FolderNotFoundException(String folderPath) {
        super("Folder not found in catalog: " + folderPath);
    }
}
