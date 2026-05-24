package com.jpablodrexler.photomanager.domain.port.out.playlist;

import com.jpablodrexler.photomanager.domain.model.Asset;

import java.nio.file.Path;
import java.util.List;

public interface PlaylistParserPort {

    boolean supports(String fileName);

    List<Asset> parse(Path playlistPath);
}
