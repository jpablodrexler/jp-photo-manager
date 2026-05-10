package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.application.dto.ConvertAssetsResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface ConvertAssetsService {

    CompletableFuture<List<ConvertAssetsResult>> executeAsync(Consumer<String> statusCallback);
}
