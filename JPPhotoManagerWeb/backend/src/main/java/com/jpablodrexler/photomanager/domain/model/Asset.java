package com.jpablodrexler.photomanager.domain.model;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    public String getThumbnailBlobName() {
        return assetId + ".bin";
    }

    public String getFullPath() {
        return folder != null ? folder.getPath() + "/" + fileName : fileName;
    }
}
