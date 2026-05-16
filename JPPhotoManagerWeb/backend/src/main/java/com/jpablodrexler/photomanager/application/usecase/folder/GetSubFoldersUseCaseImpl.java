package com.jpablodrexler.photomanager.application.usecase.folder;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetSubFoldersUseCase;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetSubFoldersUseCaseImpl implements GetSubFoldersUseCase {

    private final FolderRepository folderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Folder> execute(String parentPath) {
        if (parentPath == null || parentPath.isBlank()) {
            return folderRepository.findAll();
        }
        return folderRepository.findSubFolders(parentPath + "/");
    }
}
