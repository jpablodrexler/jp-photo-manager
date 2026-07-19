package com.jpablodrexler.photomanager.application.usecase.folder;

import com.jpablodrexler.photomanager.domain.model.RecentTargetPath;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetRecentTargetPathsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.RecentTargetPathRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetRecentTargetPathsUseCaseImpl implements GetRecentTargetPathsUseCase {

    private final RecentTargetPathRepository recentTargetPathRepository;

    @Override
    @Transactional(readOnly = true)
    public List<String> execute() {
        return recentTargetPathRepository.findAllOrderByIdDesc().stream()
                .map(RecentTargetPath::getPath)
                .collect(Collectors.toList());
    }
}
