package com.jpablodrexler.photomanager.application.usecase.asset;

import com.jpablodrexler.photomanager.domain.model.RenameAssetsResult;
import com.jpablodrexler.photomanager.domain.model.RenamePreview;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.port.in.asset.RenameAssetsUseCase;
import com.jpablodrexler.photomanager.domain.port.out.AssetRepository;
import com.jpablodrexler.photomanager.domain.port.out.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RenameAssetsUseCaseImpl implements RenameAssetsUseCase {

    private static final Pattern DATE_TOKEN = Pattern.compile("\\{date:([^}]+)\\}");
    private static final Pattern INDEX_TOKEN = Pattern.compile("\\{index:(\\d+)d\\}");
    private static final String ORIGINAL_TOKEN = "{original}";
    private static final String EXT_TOKEN = "{ext}";
    // Allowlist: only safe DateTimeFormatter pattern characters
    private static final Pattern SAFE_DATE_FORMAT = Pattern.compile("[a-zA-Z0-9\\-_.:/ ]+");

    private final AssetRepository assetRepository;
    private final StoragePort storagePort;
    private final PlatformTransactionManager transactionManager;

    @Override
    public RenameAssetsResult execute(Long[] assetIds, String pattern, boolean applied) {
        validateDateFormat(pattern);
        List<Asset> assets = assetRepository.findAllById(Arrays.asList(assetIds));
        List<RenamePreview> previews = buildPreviews(assets, pattern);
        checkWithinBatchCollisions(previews);
        if (applied) {
            checkFolderCollisions(previews, assets);
            applyRenames(assets, previews);
        }
        return new RenameAssetsResult(previews, applied);
    }

    private void validateDateFormat(String pattern) {
        Matcher m = DATE_TOKEN.matcher(pattern);
        while (m.find()) {
            String fmt = m.group(1);
            if (!SAFE_DATE_FORMAT.matcher(fmt).matches()) {
                throw new IllegalArgumentException("INVALID_DATE_FORMAT");
            }
            try {
                DateTimeFormatter.ofPattern(fmt);
            } catch (Exception e) {
                throw new IllegalArgumentException("INVALID_DATE_FORMAT");
            }
        }
    }

    private List<RenamePreview> buildPreviews(List<Asset> assets, String pattern) {
        List<RenamePreview> previews = new ArrayList<>();
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            String newName = resolvePattern(pattern, asset, i + 1);
            previews.add(new RenamePreview(asset.getAssetId(), asset.getFileName(), newName));
        }
        return previews;
    }

    private String resolvePattern(String pattern, Asset asset, int index) {
        String result = pattern;

        // {date:FORMAT}
        Matcher dateMatcher = DATE_TOKEN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (dateMatcher.find()) {
            String fmt = dateMatcher.group(1);
            String formatted = asset.getFileCreationDateTime() != null
                    ? DateTimeFormatter.ofPattern(fmt).format(asset.getFileCreationDateTime())
                    : "";
            dateMatcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        dateMatcher.appendTail(sb);
        result = sb.toString();

        // {index:PAD}
        Matcher indexMatcher = INDEX_TOKEN.matcher(result);
        sb = new StringBuffer();
        while (indexMatcher.find()) {
            int pad = Integer.parseInt(indexMatcher.group(1));
            String formatted = String.format("%0" + pad + "d", index);
            indexMatcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        indexMatcher.appendTail(sb);
        result = sb.toString();

        // {original} — filename without extension
        result = result.replace(ORIGINAL_TOKEN, baseNameOf(asset.getFileName()));

        // {ext} — lowercase extension
        String ext = extensionOf(asset.getFileName());
        result = result.replace(EXT_TOKEN, ext);

        // Auto-append extension when {ext} is absent from original pattern
        if (!pattern.contains(EXT_TOKEN) && !ext.isEmpty()) {
            result = result + "." + ext;
        }

        return result;
    }

    private String baseNameOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(0, dot) : fileName;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private void checkWithinBatchCollisions(List<RenamePreview> previews) {
        Set<String> seen = new HashSet<>();
        for (RenamePreview preview : previews) {
            if (!seen.add(preview.newName())) {
                throw new IllegalArgumentException("ASSET_NAME_COLLISION");
            }
        }
    }

    private void checkFolderCollisions(List<RenamePreview> previews, List<Asset> assets) {
        Set<Long> batchIds = assets.stream().map(Asset::getAssetId).collect(Collectors.toSet());

        // Group target names by folder path to make one DB call per folder
        Map<String, Set<String>> targetNamesByFolderPath = new HashMap<>();
        for (int i = 0; i < assets.size(); i++) {
            String folderPath = assets.get(i).getFolder().getPath();
            targetNamesByFolderPath
                    .computeIfAbsent(folderPath, k -> new HashSet<>())
                    .add(previews.get(i).newName());
        }

        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            Set<String> folderTargets = targetNamesByFolderPath.get(asset.getFolder().getPath());
            List<Asset> folderAssets = assetRepository.findByFolder(asset.getFolder());
            for (Asset existing : folderAssets) {
                if (!batchIds.contains(existing.getAssetId())
                        && folderTargets.contains(existing.getFileName())) {
                    throw new IllegalArgumentException("ASSET_NAME_COLLISION");
                }
            }
            // Only query each folder once
            targetNamesByFolderPath.remove(asset.getFolder().getPath());
        }
    }

    private void applyRenames(List<Asset> assets, List<RenamePreview> previews) {
        TransactionTemplate perAssetTransaction = new TransactionTemplate(transactionManager);
        perAssetTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            String newName = previews.get(i).newName();
            String oldPath = asset.getFolder().getPath() + "/" + asset.getFileName();
            String newPath = asset.getFolder().getPath() + "/" + newName;
            try {
                storagePort.moveFile(oldPath, newPath);
            } catch (IOException e) {
                log.error("Failed to rename asset {} to {}", oldPath, newPath, e);
                throw new RuntimeException("Failed to rename asset: " + oldPath, e);
            }

            try {
                perAssetTransaction.executeWithoutResult(status -> {
                    asset.setFileName(newName);
                    assetRepository.save(asset);
                });
                log.info("Renamed asset {} → {}", oldPath, newPath);
            } catch (RuntimeException e) {
                log.error("Failed to persist rename for asset {}, reverting file on disk", asset.getAssetId(), e);
                revertRename(newPath, oldPath, asset.getAssetId());
                throw e;
            }
        }
    }

    private void revertRename(String newPath, String oldPath, Long assetId) {
        try {
            storagePort.moveFile(newPath, oldPath);
        } catch (IOException undoEx) {
            log.error("Failed to revert file rename for asset {} after DB save failure", assetId, undoEx);
        }
    }
}
