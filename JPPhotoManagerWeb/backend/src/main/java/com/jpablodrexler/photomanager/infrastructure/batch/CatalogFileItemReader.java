package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.FolderRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class CatalogFileItemReader implements ItemReader<Path> {

    private final String folderPath;
    private final AssetRepository assetRepository;
    private final FolderRepository folderRepository;
    private final StoragePort storagePort;

    private Iterator<Path> fileIterator;

    public CatalogFileItemReader(String folderPath, AssetRepository assetRepository,
                                  FolderRepository folderRepository, StoragePort storagePort) {
        this.folderPath = folderPath;
        this.assetRepository = assetRepository;
        this.folderRepository = folderRepository;
        this.storagePort = storagePort;
        init();
    }

    private void init() {
        Optional<Folder> folder = folderRepository.findByPath(folderPath);

        Set<String> cataloguedFileNames = folder
                .map(f -> assetRepository.findByFolder(f).stream()
                        .map(a -> a.getFileName())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());

        List<String> allFiles = storagePort.listFiles(folderPath);
        log.debug("Folder {} has {} files on disk, {} already catalogued",
                folderPath, allFiles.size(), cataloguedFileNames.size());

        fileIterator = allFiles.stream()
                .map(Paths::get)
                .filter(p -> !cataloguedFileNames.contains(p.getFileName().toString()))
                .iterator();
    }

    @Override
    public synchronized Path read() {
        return fileIterator.hasNext() ? fileIterator.next() : null;
    }
}
