package com.jpablodrexler.photomanager.domain.service;

import com.jpablodrexler.photomanager.application.dto.SyncAssetsResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface SyncAssetsService {

    CompletableFuture<List<SyncAssetsResult>> executeAsync(Consumer<String> statusCallback);
}
