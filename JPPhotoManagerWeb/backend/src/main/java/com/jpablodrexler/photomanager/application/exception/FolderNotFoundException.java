package com.jpablodrexler.photomanager.application.exception;

public class FolderNotFoundException extends RuntimeException {
    public FolderNotFoundException(String folderPath) {
        super("Folder not found in catalog: " + folderPath);
    }
}
