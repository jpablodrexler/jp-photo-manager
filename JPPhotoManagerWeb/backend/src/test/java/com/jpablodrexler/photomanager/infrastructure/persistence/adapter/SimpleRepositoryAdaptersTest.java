package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.*;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.*;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.*;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.*;


import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimpleRepositoryAdaptersTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class FolderRepositoryImplTest {

        @Mock JpaFolderRepository jpa;
        @Mock FolderEntityMapper mapper;
        @InjectMocks FolderRepositoryImpl sut;

        @Test
        void findById_present_returnsMappedDomain() {
            FolderEntity entity = new FolderEntity();
            Folder domain = Folder.builder().folderId(1L).path("/photos").build();
            when(jpa.findById(1L)).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findById(1L)).contains(domain);
        }

        @Test
        void findByPath_present_returnsMappedDomain() {
            FolderEntity entity = new FolderEntity();
            Folder domain = Folder.builder().folderId(1L).path("/photos").build();
            when(jpa.findByPath("/photos")).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findByPath("/photos")).contains(domain);
        }

        @Test
        void existsByPath_delegatesToJpa() {
            when(jpa.existsByPath("/photos")).thenReturn(true);
            assertThat(sut.existsByPath("/photos")).isTrue();
        }

        @Test
        void findAll_returnsMappedList() {
            FolderEntity entity = new FolderEntity();
            Folder domain = Folder.builder().folderId(1L).build();
            when(jpa.findAll()).thenReturn(List.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findAll()).containsExactly(domain);
        }

        @Test
        void findSubFolders_returnsMappedList() {
            FolderEntity entity = new FolderEntity();
            Folder domain = Folder.builder().folderId(2L).path("/photos/2024").build();
            when(jpa.findSubFolders("/photos/")).thenReturn(List.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findSubFolders("/photos/")).containsExactly(domain);
        }

        @Test
        void save_mapsAndPersists() {
            Folder folder = Folder.builder().folderId(1L).path("/photos").build();
            FolderEntity entity = new FolderEntity();
            when(mapper.toEntity(folder)).thenReturn(entity);
            when(jpa.save(entity)).thenReturn(entity);
            when(mapper.toDomain(entity)).thenReturn(folder);

            assertThat(sut.save(folder)).isEqualTo(folder);
        }

        @Test
        void deleteById_delegatesToJpa() {
            sut.deleteById(1L);
            verify(jpa).deleteById(1L);
        }

        @Test
        void count_delegatesToJpa() {
            when(jpa.count()).thenReturn(10L);
            assertThat(sut.count()).isEqualTo(10L);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class SyncConfigRepositoryImplTest {

        @Mock JpaSyncConfigRepository jpa;
        @Mock SyncConfigEntityMapper mapper;
        @InjectMocks SyncConfigRepositoryImpl sut;

        @Test
        void findAllOrderByOrder_returnsMappedList() {
            SyncAssetsDirectoriesDefinitionEntity entity = new SyncAssetsDirectoriesDefinitionEntity();
            SyncDirectoriesDefinition domain = SyncDirectoriesDefinition.builder()
                    .sourceDirectory("/src").destinationDirectory("/dst").build();
            when(jpa.findAllByOrderByOrderAsc()).thenReturn(List.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findAllOrderByOrder()).containsExactly(domain);
        }

        @Test
        void saveAll_deletesExistingAndSavesNew() {
            SyncDirectoriesDefinition domain = SyncDirectoriesDefinition.builder()
                    .sourceDirectory("/src").destinationDirectory("/dst").build();
            SyncAssetsDirectoriesDefinitionEntity entity = new SyncAssetsDirectoriesDefinitionEntity();
            when(mapper.toEntity(domain)).thenReturn(entity);

            sut.saveAll(List.of(domain));

            verify(jpa).deleteAllInBatch();
            verify(jpa).saveAll(List.of(entity));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ConvertConfigRepositoryImplTest {

        @Mock JpaConvertConfigRepository jpa;
        @Mock ConvertConfigEntityMapper mapper;
        @InjectMocks ConvertConfigRepositoryImpl sut;

        @Test
        void findAllOrderByOrder_returnsMappedList() {
            ConvertAssetsDirectoriesDefinitionEntity entity = new ConvertAssetsDirectoriesDefinitionEntity();
            ConvertDirectoriesDefinition domain = ConvertDirectoriesDefinition.builder()
                    .sourceDirectory("/src").destinationDirectory("/dst").build();
            when(jpa.findAllByOrderByOrderAsc()).thenReturn(List.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findAllOrderByOrder()).containsExactly(domain);
        }

        @Test
        void saveAll_deletesExistingAndSavesNew() {
            ConvertDirectoriesDefinition domain = ConvertDirectoriesDefinition.builder()
                    .sourceDirectory("/src").destinationDirectory("/dst").build();
            ConvertAssetsDirectoriesDefinitionEntity entity = new ConvertAssetsDirectoriesDefinitionEntity();
            when(mapper.toEntity(domain)).thenReturn(entity);

            sut.saveAll(List.of(domain));

            verify(jpa).deleteAllInBatch();
            verify(jpa).saveAll(List.of(entity));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class SearchPresetRepositoryImplTest {

        @Mock JpaSearchPresetRepository jpa;
        @Mock SearchPresetEntityMapper mapper;
        @InjectMocks SearchPresetRepositoryImpl sut;

        @Test
        void findByUserIdOrderByCreatedAtDesc_returnsMappedList() {
            UUID userId = UUID.randomUUID();
            SearchPresetEntity entity = new SearchPresetEntity();
            SearchPreset domain = SearchPreset.builder().presetId(1L).name("My Preset").build();
            when(jpa.findByUser_IdOrderByCreatedAtDesc(userId)).thenReturn(List.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findByUserIdOrderByCreatedAtDesc(userId)).containsExactly(domain);
        }

        @Test
        void findByIdAndUserId_present_returnsMappedDomain() {
            UUID userId = UUID.randomUUID();
            SearchPresetEntity entity = new SearchPresetEntity();
            SearchPreset domain = SearchPreset.builder().presetId(1L).build();
            when(jpa.findByPresetIdAndUser_Id(1L, userId)).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findByIdAndUserId(1L, userId)).contains(domain);
        }

        @Test
        void deleteById_delegatesToJpa() {
            sut.deleteById(5L);
            verify(jpa).deleteById(5L);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class RefreshTokenRepositoryImplTest {

        @Mock JpaRefreshTokenRepository jpa;
        @Mock RefreshTokenEntityMapper mapper;
        @InjectMocks RefreshTokenRepositoryImpl sut;

        @Test
        void findByToken_present_returnsMappedDomain() {
            RefreshTokenEntity entity = new RefreshTokenEntity();
            RefreshToken domain = RefreshToken.builder().tokenId(1L).token("tok").build();
            when(jpa.findByToken("tok")).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findByToken("tok")).contains(domain);
        }

        @Test
        void save_mapsAndPersists() {
            RefreshToken token = RefreshToken.builder().tokenId(1L).token("tok").build();
            RefreshTokenEntity entity = new RefreshTokenEntity();
            when(mapper.toEntity(token)).thenReturn(entity);
            when(jpa.save(entity)).thenReturn(entity);
            when(mapper.toDomain(entity)).thenReturn(token);

            assertThat(sut.save(token)).isEqualTo(token);
        }

        @Test
        void deleteByUserId_delegatesToJpa() {
            UUID userId = UUID.randomUUID();
            sut.deleteByUserId(userId);
            verify(jpa).deleteByUser_Id(userId);
        }

        @Test
        void deleteById_delegatesToJpa() {
            sut.deleteById(1L);
            verify(jpa).deleteById(1L);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class RecentTargetPathRepositoryImplTest {

        @Mock JpaRecentTargetPathRepository jpa;
        @InjectMocks RecentTargetPathRepositoryImpl sut;

        @Test
        void findAllOrderByIdDesc_returnsMappedList() {
            RecentTargetPathEntity entity = new RecentTargetPathEntity("/foo");
            entity.setId(1L);
            when(jpa.findAllByOrderByIdDesc()).thenReturn(List.of(entity));

            List<RecentTargetPath> result = sut.findAllOrderByIdDesc();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPath()).isEqualTo("/foo");
        }

        @Test
        void existsByPath_delegatesToJpa() {
            when(jpa.existsByPath("/foo")).thenReturn(true);
            assertThat(sut.existsByPath("/foo")).isTrue();
        }

        @Test
        void save_mapsAndPersists() {
            RecentTargetPath path = RecentTargetPath.builder().path("/bar").build();
            RecentTargetPathEntity entity = new RecentTargetPathEntity("/bar");
            entity.setId(2L);
            when(jpa.save(any(RecentTargetPathEntity.class))).thenReturn(entity);

            RecentTargetPath result = sut.save(path);

            assertThat(result.getPath()).isEqualTo("/bar");
            assertThat(result.getId()).isEqualTo(2L);
        }

        @Test
        void deleteById_delegatesToJpa() {
            sut.deleteById(1L);
            verify(jpa).deleteById(1L);
        }

        @Test
        void deleteAll_delegatesToJpaWithIds() {
            RecentTargetPath p1 = RecentTargetPath.builder().id(1L).path("/a").build();
            RecentTargetPath p2 = RecentTargetPath.builder().id(2L).path("/b").build();

            sut.deleteAll(List.of(p1, p2));

            verify(jpa).deleteAllById(List.of(1L, 2L));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class UserRepositoryImplTest {

        @Mock JpaUserRepository jpa;
        @Mock UserEntityMapper mapper;
        @InjectMocks UserRepositoryImpl sut;

        @Test
        void findById_present_returnsMappedDomain() {
            UUID id = UUID.randomUUID();
            com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity entity =
                    new com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity();
            User domain = User.builder().id(id).username("alice").build();
            when(jpa.findById(id)).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findById(id)).contains(domain);
        }

        @Test
        void findByUsername_present_returnsMappedDomain() {
            com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity entity =
                    new com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity();
            User domain = User.builder().id(UUID.randomUUID()).username("alice").build();
            when(jpa.findByUsername("alice")).thenReturn(Optional.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findByUsername("alice")).contains(domain);
        }

        @Test
        void findAll_returnsMappedList() {
            com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity entity =
                    new com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity();
            User domain = User.builder().id(UUID.randomUUID()).username("alice").build();
            when(jpa.findAll()).thenReturn(List.of(entity));
            when(mapper.toDomain(entity)).thenReturn(domain);

            assertThat(sut.findAll()).containsExactly(domain);
        }

        @Test
        void save_mapsAndPersists() {
            User user = User.builder().id(UUID.randomUUID()).username("bob").build();
            com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity entity =
                    new com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity();
            when(mapper.toEntity(user)).thenReturn(entity);
            when(jpa.save(entity)).thenReturn(entity);
            when(mapper.toDomain(entity)).thenReturn(user);

            assertThat(sut.save(user)).isEqualTo(user);
        }

        @Test
        void deleteById_delegatesToJpa() {
            UUID id = UUID.randomUUID();
            sut.deleteById(id);
            verify(jpa).deleteById(id);
        }

        @Test
        void count_delegatesToJpa() {
            when(jpa.count()).thenReturn(3L);
            assertThat(sut.count()).isEqualTo(3L);
        }
    }
}
