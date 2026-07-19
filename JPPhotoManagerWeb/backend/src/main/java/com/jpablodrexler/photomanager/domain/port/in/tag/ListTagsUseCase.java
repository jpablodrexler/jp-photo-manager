package com.jpablodrexler.photomanager.domain.port.in.tag;

import com.jpablodrexler.photomanager.domain.model.Tag;

import java.util.List;

public interface ListTagsUseCase {
    List<Tag> execute(String query);
}
