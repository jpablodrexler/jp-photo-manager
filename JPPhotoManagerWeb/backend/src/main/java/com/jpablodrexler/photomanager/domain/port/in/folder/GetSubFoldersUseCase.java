package com.jpablodrexler.photomanager.domain.port.in.folder;

import com.jpablodrexler.photomanager.domain.model.Folder;
import java.util.List;

public interface GetSubFoldersUseCase {
    List<Folder> execute(String parentPath);
}
