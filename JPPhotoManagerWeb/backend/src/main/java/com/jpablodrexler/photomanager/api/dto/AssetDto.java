package com.jpablodrexler.photomanager.api.dto;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssetDto {

    private Long assetId;
    private Long folderId;
    private String folderPath;
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
    private String thumbnailUrl;
    private String imageUrl;
}
