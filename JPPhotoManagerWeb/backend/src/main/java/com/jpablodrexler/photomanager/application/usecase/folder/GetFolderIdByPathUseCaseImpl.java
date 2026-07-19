package com.jpablodrexler.photomanager.application.usecase.folder;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetFolderIdByPathUseCase;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetFolderIdByPathUseCaseImpl implements GetFolderIdByPathUseCase {

    private final FolderRepository folderRepository;

    @Override
    @Transactional(readOnly = true)
    public Long execute(String folderPath) {
        return folderRepository.findByPath(folderPath).map(Folder::getFolderId).orElse(null);
    }
}
