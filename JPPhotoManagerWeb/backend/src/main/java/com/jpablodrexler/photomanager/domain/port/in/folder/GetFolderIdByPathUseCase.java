package com.jpablodrexler.photomanager.domain.port.in.folder;

public interface GetFolderIdByPathUseCase {
    Long execute(String folderPath);
}
