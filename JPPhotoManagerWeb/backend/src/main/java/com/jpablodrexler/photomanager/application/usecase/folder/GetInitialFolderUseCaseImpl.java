package com.jpablodrexler.photomanager.application.usecase.folder;

import com.jpablodrexler.photomanager.domain.port.in.folder.GetInitialFolderUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GetInitialFolderUseCaseImpl implements GetInitialFolderUseCase {

    @Value("${photomanager.initial-directory:${user.home}/Pictures}")
    private String initialDirectory;

    @Override
    public String execute() {
        return initialDirectory;
    }
}
