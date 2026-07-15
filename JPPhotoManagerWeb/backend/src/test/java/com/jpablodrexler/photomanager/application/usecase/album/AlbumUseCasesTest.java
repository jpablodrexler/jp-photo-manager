package com.jpablodrexler.photomanager.application.usecase.album;

import com.jpablodrexler.photomanager.domain.model.AlbumData;
import com.jpablodrexler.photomanager.domain.model.AssetFilter;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.application.exception.AlbumNotFoundException;
import com.jpablodrexler.photomanager.application.exception.SmartAlbumMembershipException;
import com.jpablodrexler.photomanager.application.exception.UserNotFoundException;
import com.jpablodrexler.photomanager.domain.model.Album;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.AlbumRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlbumUseCasesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class AddAssetsToAlbumUseCaseImplTest {

        @Mock AlbumRepository albumRepository;
        @InjectMocks AddAssetsToAlbumUseCaseImpl sut;

        @Test
        void execute_albumFound_addsAssets() {
            Long albumId = 1L;
            UUID userId = UUID.randomUUID();
            List<Long> assetIds = List.of(10L, 11L);
            Album album = Album.builder().albumId(albumId).userId(userId).build();
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.of(album));

            sut.execute(albumId, userId, assetIds);

            verify(albumRepository).addAssets(albumId, assetIds);
        }

        @Test
        void execute_albumNotFound_throwsAlbumNotFoundException() {
            Long albumId = 99L;
            UUID userId = UUID.randomUUID();
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(albumId, userId, List.of(1L)))
                    .isInstanceOf(AlbumNotFoundException.class);
            verify(albumRepository, never()).addAssets(any(), any());
        }

        @Test
        void execute_smartAlbum_throwsSmartAlbumMembershipException() {
            Long albumId = 7L;
            UUID userId = UUID.randomUUID();
            Album album = Album.builder().albumId(albumId).userId(userId).filterJson("{\"minRating\":3}").build();
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> sut.execute(albumId, userId, List.of(101L, 102L)))
                    .isInstanceOf(SmartAlbumMembershipException.class);
            verify(albumRepository, never()).addAssets(any(), any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class CreateAlbumUseCaseImplTest {

        @Mock AlbumRepository albumRepository;
        @Mock UserRepository userRepository;
        @InjectMocks CreateAlbumUseCaseImpl sut;

        @Test
        void execute_userFound_returnsAlbumData() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).username("alice").build();
            Instant now = Instant.now();
            Album saved = Album.builder().albumId(5L).userId(userId).name("Vacation").description("desc").createdAt(now).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(albumRepository.save(any())).thenReturn(saved);

            AlbumData result = sut.execute(userId, "Vacation", "desc", null);

            assertThat(result.albumId()).isEqualTo(5L);
            assertThat(result.name()).isEqualTo("Vacation");
            assertThat(result.description()).isEqualTo("desc");
            assertThat(result.assetCount()).isZero();
        }

        @Test
        void execute_userNotFound_throwsUserNotFoundException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(userId, "Album", null, null))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining(userId.toString());
        }

        @Test
        void execute_withFilterJson_storesSerializedJson() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).username("alice").build();
            String filterJsonStr = "{\"minRating\":4}";
            Instant now = Instant.now();
            Album saved = Album.builder().albumId(10L).userId(userId).name("Top Picks").createdAt(now).filterJson(filterJsonStr).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            ArgumentCaptor<Album> captor = ArgumentCaptor.forClass(Album.class);
            when(albumRepository.save(captor.capture())).thenReturn(saved);

            AlbumData result = sut.execute(userId, "Top Picks", null, filterJsonStr);

            assertThat(captor.getValue().getFilterJson()).isEqualTo(filterJsonStr);
            assertThat(result.filterJson()).isEqualTo(filterJsonStr);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class DeleteAlbumUseCaseImplTest {

        @Mock AlbumRepository albumRepository;
        @InjectMocks DeleteAlbumUseCaseImpl sut;

        @Test
        void execute_albumFound_deletesAlbum() {
            Long albumId = 3L;
            UUID userId = UUID.randomUUID();
            Album album = Album.builder().albumId(albumId).userId(userId).build();
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.of(album));

            sut.execute(albumId, userId);

            verify(albumRepository).deleteById(albumId);
        }

        @Test
        void execute_albumNotFound_throwsAlbumNotFoundException() {
            Long albumId = 99L;
            UUID userId = UUID.randomUUID();
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(albumId, userId))
                    .isInstanceOf(AlbumNotFoundException.class);
            verify(albumRepository, never()).deleteById(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetAlbumsUseCaseImplTest {

        @Mock AlbumRepository albumRepository;
        @Mock AlbumAssetFilterFactory albumAssetFilterFactory;
        @InjectMocks GetAlbumsUseCaseImpl sut;

        @Test
        void execute_returnsAlbumDataList() {
            UUID userId = UUID.randomUUID();
            Instant now = Instant.now();
            Album a1 = Album.builder().albumId(1L).name("A").createdAt(now).build();
            Album a2 = Album.builder().albumId(2L).name("B").createdAt(now).build();
            when(albumRepository.findByUserId(userId)).thenReturn(List.of(a1, a2));
            when(albumRepository.countAssets(1L)).thenReturn(3L);
            when(albumRepository.countAssets(2L)).thenReturn(7L);

            List<AlbumData> result = sut.execute(userId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).albumId()).isEqualTo(1L);
            assertThat(result.get(0).assetCount()).isEqualTo(3L);
            assertThat(result.get(1).albumId()).isEqualTo(2L);
            assertThat(result.get(1).assetCount()).isEqualTo(7L);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetAlbumSummaryUseCaseImplTest {

        @Mock AlbumRepository albumRepository;
        @Mock AlbumAssetFilterFactory albumAssetFilterFactory;
        @InjectMocks GetAlbumSummaryUseCaseImpl sut;

        @Test
        void execute_albumFound_returnsData() {
            Long albumId = 1L;
            UUID userId = UUID.randomUUID();
            Album album = Album.builder().albumId(albumId).name("Wedding").createdAt(Instant.now()).build();
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.of(album));
            when(albumRepository.countAssets(albumId)).thenReturn(10L);

            AlbumData result = sut.execute(albumId, userId);

            assertThat(result.albumId()).isEqualTo(albumId);
            assertThat(result.name()).isEqualTo("Wedding");
            assertThat(result.assetCount()).isEqualTo(10L);
        }

        @Test
        void execute_albumNotFound_throwsAlbumNotFoundException() {
            UUID userId = UUID.randomUUID();
            when(albumRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(99L, userId))
                    .isInstanceOf(AlbumNotFoundException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetAlbumAssetsUseCaseImplTest {

        @Mock AlbumRepository albumRepository;
        @Mock AlbumAssetFilterFactory albumAssetFilterFactory;
        @InjectMocks GetAlbumAssetsUseCaseImpl sut;

        @Test
        void execute_staticAlbum_callsJoinTable() {
            Long albumId = 1L;
            UUID userId = UUID.randomUUID();
            Album album = Album.builder().albumId(albumId).build();
            PaginatedResult<Asset> paginated = new PaginatedResult<>(List.of(), 0L, 0, 50);
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.of(album));
            when(albumRepository.findAssetsByAlbumId(eq(albumId), eq(0), eq(50))).thenReturn(paginated);

            PaginatedResult<Asset> result = sut.execute(albumId, userId, 0);

            assertThat(result).isEqualTo(paginated);
            verify(albumRepository).findAssetsByAlbumId(eq(albumId), eq(0), eq(50));
            verify(albumRepository, never()).findSmartAlbumAssets(any(), eq(0), eq(50));
        }

        @Test
        void execute_smartAlbum_callsSmartAlbumAssets() {
            Long albumId = 2L;
            UUID userId = UUID.randomUUID();
            Album album = Album.builder().albumId(albumId).filterJson("{\"minRating\":4}").build();
            PaginatedResult<Asset> paginated = new PaginatedResult<>(List.of(), 0L, 0, 50);
            AssetFilter filter = new AssetFilter(null, null, null, null, 4, null, 0, 50, false, Set.of());
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.of(album));
            when(albumAssetFilterFactory.build(album.getFilterJson(), 0, 50)).thenReturn(filter);
            when(albumRepository.findSmartAlbumAssets(any(AssetFilter.class), eq(0), eq(50))).thenReturn(paginated);

            PaginatedResult<Asset> result = sut.execute(albumId, userId, 0);

            assertThat(result).isEqualTo(paginated);
            verify(albumRepository).findSmartAlbumAssets(any(AssetFilter.class), eq(0), eq(50));
            verify(albumRepository, never()).findAssetsByAlbumId(any(), eq(0), eq(50));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class UpdateAlbumUseCaseImplTest {

        @Mock AlbumRepository albumRepository;
        @Mock AlbumAssetFilterFactory albumAssetFilterFactory;
        @InjectMocks UpdateAlbumUseCaseImpl sut;

        @Test
        void execute_albumFound_updatesAndReturnsData() {
            Long albumId = 1L;
            UUID userId = UUID.randomUUID();
            Album album = Album.builder().albumId(albumId).name("Old").createdAt(Instant.now()).build();
            Album saved = Album.builder().albumId(albumId).name("New").description("d").createdAt(album.getCreatedAt()).build();
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.of(album));
            when(albumRepository.save(album)).thenReturn(saved);
            when(albumRepository.countAssets(albumId)).thenReturn(5L);

            AlbumData result = sut.execute(albumId, userId, "New", "d", null);

            assertThat(result.name()).isEqualTo("New");
            assertThat(result.assetCount()).isEqualTo(5L);
        }

        @Test
        void execute_albumNotFound_throwsAlbumNotFoundException() {
            UUID userId = UUID.randomUUID();
            when(albumRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(99L, userId, "Name", null, null))
                    .isInstanceOf(AlbumNotFoundException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class RemoveAssetsFromAlbumUseCaseImplTest {

        @Mock AlbumRepository albumRepository;
        @InjectMocks RemoveAssetsFromAlbumUseCaseImpl sut;

        @Test
        void execute_albumFound_removesAssets() {
            Long albumId = 1L;
            UUID userId = UUID.randomUUID();
            List<Long> assetIds = List.of(20L, 21L);
            Album album = Album.builder().albumId(albumId).userId(userId).build();
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.of(album));

            sut.execute(albumId, userId, assetIds);

            verify(albumRepository).removeAssets(albumId, assetIds);
        }

        @Test
        void execute_albumNotFound_throwsAlbumNotFoundException() {
            UUID userId = UUID.randomUUID();
            when(albumRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(99L, userId, List.of(1L)))
                    .isInstanceOf(AlbumNotFoundException.class);
            verify(albumRepository, never()).removeAssets(any(), any());
        }

        @Test
        void execute_smartAlbum_throwsSmartAlbumMembershipException() {
            Long albumId = 7L;
            UUID userId = UUID.randomUUID();
            Album album = Album.builder().albumId(albumId).userId(userId).filterJson("{\"minRating\":3}").build();
            when(albumRepository.findByIdAndUserId(albumId, userId)).thenReturn(Optional.of(album));

            assertThatThrownBy(() -> sut.execute(albumId, userId, List.of(101L)))
                    .isInstanceOf(SmartAlbumMembershipException.class);
            verify(albumRepository, never()).removeAssets(any(), any());
        }
    }
}
