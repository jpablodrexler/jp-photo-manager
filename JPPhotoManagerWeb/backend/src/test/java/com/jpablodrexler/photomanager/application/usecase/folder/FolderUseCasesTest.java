package com.jpablodrexler.photomanager.application.usecase.folder;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.RecentTargetPath;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.RecentTargetPathRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FolderUseCasesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetDrivesUseCaseImplTest {

        @InjectMocks GetDrivesUseCaseImpl sut;

        @Test
        void execute_returnsAtLeastOneRoot() {
            List<String> drives = sut.execute();
            assertThat(drives).isNotEmpty();
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetSubFoldersUseCaseImplTest {

        @Mock FolderRepository folderRepository;
        @InjectMocks GetSubFoldersUseCaseImpl sut;

        @Test
        void execute_nullParent_returnsAllFolders() {
            sut.execute(null);
            verify(folderRepository).findAll();
        }

        @Test
        void execute_blankParent_returnsAllFolders() {
            sut.execute("   ");
            verify(folderRepository).findAll();
        }

        @Test
        void execute_withParentPath_returnsSubFolders() {
            Folder sub = Folder.builder().folderId(1L).path("/photos/2024").build();
            when(folderRepository.findSubFolders("/photos/")).thenReturn(List.of(sub));

            List<Folder> result = sut.execute("/photos");

            assertThat(result).containsExactly(sub);
            verify(folderRepository).findSubFolders("/photos/");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetRecentTargetPathsUseCaseImplTest {

        @Mock RecentTargetPathRepository recentTargetPathRepository;
        @InjectMocks GetRecentTargetPathsUseCaseImpl sut;

        @Test
        void execute_returnsPaths() {
            when(recentTargetPathRepository.findAllOrderByIdDesc()).thenReturn(
                    List.of(RecentTargetPath.builder().id(1L).path("/foo").build(),
                            RecentTargetPath.builder().id(2L).path("/bar").build()));

            List<String> result = sut.execute();

            assertThat(result).containsExactly("/foo", "/bar");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class GetInitialFolderUseCaseImplTest {

        @InjectMocks GetInitialFolderUseCaseImpl sut;

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(sut, "initialDirectory", "/test/pictures");
        }

        @Test
        void execute_returnsConfiguredDirectory() {
            assertThat(sut.execute()).isEqualTo("/test/pictures");
        }
    }
}
