package com.jpablodrexler.photomanager.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssetResponseDto {

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
    private int rating;
    private List<String> tags;
    private FileType fileType;
    @JsonProperty("isVideo")
    private boolean isVideo;
}
