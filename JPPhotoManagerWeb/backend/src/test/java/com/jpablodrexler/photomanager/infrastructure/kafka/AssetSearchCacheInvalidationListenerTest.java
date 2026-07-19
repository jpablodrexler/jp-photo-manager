package com.jpablodrexler.photomanager.infrastructure.kafka;

import com.jpablodrexler.photomanager.application.dto.AssetCatalogedEvent;
import com.jpablodrexler.photomanager.application.dto.AssetDeletedEvent;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetFolderIdByPathUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetSearchCachePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetSearchCacheInvalidationListenerTest {

    @Mock AssetSearchCachePort assetSearchCachePort;
    @Mock GetFolderIdByPathUseCase getFolderIdByPathUseCase;
    @InjectMocks AssetSearchCacheInvalidationListener sut;

    @Test
    void onAssetDeleted_evictsUsingEventFolderId() {
        sut.onAssetDeleted(new AssetDeletedEvent(1L, 5L, "/photos", Instant.now(), false, null));

        verify(assetSearchCachePort).evictFolder(5L);
        verify(getFolderIdByPathUseCase, never()).execute(any());
    }

    @Test
    void onAssetCataloged_resolvesFolderIdViaUseCaseThenEvicts() {
        when(getFolderIdByPathUseCase.execute("/photos/vacation")).thenReturn(7L);

        sut.onAssetCataloged(new AssetCatalogedEvent(1L, "/photos/vacation", Instant.now(), null));

        verify(getFolderIdByPathUseCase).execute("/photos/vacation");
        verify(assetSearchCachePort).evictFolder(7L);
    }

    @Test
    void onAssetCataloged_unresolvableFolderPath_skipsEvictionWithoutThrowing() {
        when(getFolderIdByPathUseCase.execute("/unknown")).thenReturn(null);

        sut.onAssetCataloged(new AssetCatalogedEvent(1L, "/unknown", Instant.now(), null));

        verify(assetSearchCachePort, never()).evictFolder(any());
    }
}
