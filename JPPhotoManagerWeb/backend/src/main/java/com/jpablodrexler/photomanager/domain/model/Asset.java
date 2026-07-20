package com.jpablodrexler.photomanager.domain.model;

import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import com.jpablodrexler.photomanager.domain.enums.ProcessingStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    private Long assetId;
    private Folder folder;
    private String fileName;
    private long fileSize;
    private Integer pixelWidth;
    private Integer pixelHeight;
    private Integer thumbnailPixelWidth;
    private Integer thumbnailPixelHeight;
    private ImageRotation imageRotation;
    private LocalDateTime thumbnailCreationDateTime;
    private String hash;
    private LocalDateTime fileCreationDateTime;
    private LocalDateTime fileModificationDateTime;
    private LocalDateTime deletedAt;
    private int rating;
    @Builder.Default
    private Set<String> tags = new HashSet<>();
    @Builder.Default
    private FileType fileType = FileType.IMAGE;
    @Builder.Default
    private boolean isVideo = false;
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.COMPLETED;
    private LocalDateTime hashCompletedAt;
    private LocalDateTime exifCompletedAt;
    private LocalDateTime thumbnailCompletedAt;

    public String getThumbnailBlobName() {
        return assetId + ".bin";
    }

    // The thumbnail endpoint is served with a far-future, immutable Cache-Control header (see
    // AssetController.getThumbnail) — safe only because this "?v=" token changes whenever the
    // underlying thumbnail bytes do (recatalogued from scratch after a data wipe reuses low
    // asset IDs for entirely different files; a reprocess regenerates the thumbnail under the
    // same ID). Without it, browsers that already cached the old URL never re-fetch.
    public String getThumbnailUrl() {
        String base = "/api/assets/" + assetId + "/thumbnail";
        return thumbnailCreationDateTime != null
                ? base + "?v=" + thumbnailCreationDateTime.toEpochSecond(ZoneOffset.UTC)
                : base;
    }

    public String getFullPath() {
        return folder != null ? folder.getPath() + "/" + fileName : fileName;
    }
}
