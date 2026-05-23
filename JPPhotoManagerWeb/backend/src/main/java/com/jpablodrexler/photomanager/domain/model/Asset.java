package com.jpablodrexler.photomanager.domain.model;

import com.jpablodrexler.photomanager.domain.enums.FileType;
import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
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
    private Set<String> tags = new HashSet<>();
    private FileType fileType = FileType.IMAGE;

    public String getThumbnailBlobName() {
        return assetId + ".bin";
    }

    public String getFullPath() {
        return folder != null ? folder.getPath() + "/" + fileName : fileName;
    }
}
