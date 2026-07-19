package com.jpablodrexler.photomanager.application.usecase.folder;

import com.jpablodrexler.photomanager.domain.port.in.folder.GetDrivesUseCase;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GetDrivesUseCaseImpl implements GetDrivesUseCase {

    @Override
    public List<String> execute() {
        return Arrays.stream(File.listRoots())
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
    }
}
