package com.jpablodrexler.photomanager.application.usecase.tag;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.tag.ListTagsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListTagsUseCaseImpl implements ListTagsUseCase {

    private static final int MAX_RESULTS = 20;

    private final TagRepository tagRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Tag> execute(String query) {
        String q = (query == null) ? "" : query.trim();
        return tagRepository.findByNameContaining(q, MAX_RESULTS);
    }
}
