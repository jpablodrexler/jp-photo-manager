package com.jpablodrexler.photomanager.domain.port.in.convert;

import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface ConvertAssetsUseCase {
    CompletableFuture<List<ConvertAssetsResult>> execute(Consumer<String> listener);
}
