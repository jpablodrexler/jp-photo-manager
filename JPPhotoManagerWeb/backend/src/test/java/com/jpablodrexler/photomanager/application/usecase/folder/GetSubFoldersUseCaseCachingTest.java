package com.jpablodrexler.photomanager.application.usecase.folder;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.in.folder.GetSubFoldersUseCase;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code @Cacheable("sub-folders")} behavior declared on
 * {@link GetSubFoldersUseCaseImpl}, in particular that a null {@code parentPath} (the
 * root-folder-listing call made by {@code GET /api/folders} with no query parameter) does not
 * blow up — Spring's cache abstraction rejects a null SpEL-generated key.
 */
class GetSubFoldersUseCaseCachingTest {

    @Configuration
    @EnableCaching
    static class CachingTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("sub-folders");
        }

        @Bean
        FolderRepository folderRepository() {
            return mock(FolderRepository.class);
        }

        @Bean
        GetSubFoldersUseCaseImpl getSubFoldersUseCase(FolderRepository folderRepository) {
            return new GetSubFoldersUseCaseImpl(folderRepository);
        }
    }

    private AnnotationConfigApplicationContext context;
    private GetSubFoldersUseCase getSubFoldersUseCase;
    private FolderRepository folderRepository;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(CachingTestConfig.class);
        getSubFoldersUseCase = context.getBean(GetSubFoldersUseCase.class);
        folderRepository = context.getBean(FolderRepository.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void execute_withNullParentPath_doesNotThrow() {
        when(folderRepository.findAll()).thenReturn(List.of());

        assertThatCode(() -> getSubFoldersUseCase.execute(null)).doesNotThrowAnyException();
    }

    @Test
    void execute_withNullParentPathCalledTwice_secondCallServedFromCache() {
        when(folderRepository.findAll()).thenReturn(List.of());

        getSubFoldersUseCase.execute(null);
        getSubFoldersUseCase.execute(null);

        verify(folderRepository, times(1)).findAll();
    }

    @Test
    void execute_withDistinctParentPaths_cachedIndependently() {
        Folder vacationChild = Folder.builder().path("/photos/vacation/2024").build();
        Folder familyChild = Folder.builder().path("/photos/family/reunion").build();
        when(folderRepository.findSubFolders("/photos/vacation/")).thenReturn(List.of(vacationChild));
        when(folderRepository.findSubFolders("/photos/family/")).thenReturn(List.of(familyChild));

        getSubFoldersUseCase.execute("/photos/vacation");
        getSubFoldersUseCase.execute("/photos/vacation");
        getSubFoldersUseCase.execute("/photos/family");

        verify(folderRepository, times(1)).findSubFolders("/photos/vacation/");
        verify(folderRepository, times(1)).findSubFolders("/photos/family/");
    }
}
