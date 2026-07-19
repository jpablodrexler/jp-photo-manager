package com.jpablodrexler.photomanager.infrastructure.batch;

import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogFolderPartitioner implements Partitioner {

    private final StoragePort storagePort;

    @Value("${photomanager.root-catalog-folders:${user.home}/Pictures}")
    private String rootCatalogFolders;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        List<String> allFolders = new ArrayList<>();
        for (String root : rootCatalogFolders.split(";")) {
            String trimmed = root.trim();
            if (storagePort.directoryExists(trimmed)) {
                allFolders.add(trimmed);
                collectSubFolders(trimmed, allFolders);
            } else {
                log.warn("Root catalog folder does not exist: {}", trimmed);
            }
        }

        Map<String, ExecutionContext> partitions = new LinkedHashMap<>();
        for (int i = 0; i < allFolders.size(); i++) {
            ExecutionContext ctx = new ExecutionContext();
            ctx.putString("folderPath", allFolders.get(i));
            ctx.putInt("startIndex", i);
            partitions.put("partition" + i, ctx);
        }
        log.debug("Created {} partitions for catalog job", partitions.size());
        return partitions;
    }

    private void collectSubFolders(String parentPath, List<String> result) {
        List<String> subs = storagePort.listSubDirectories(parentPath);
        for (String sub : subs) {
            result.add(sub);
            collectSubFolders(sub, result);
        }
    }
}
