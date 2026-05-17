package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AssetDto;
import org.springframework.stereotype.Component;

@Component
public class AssetWebMapper {

    public AssetDto toDto(Asset asset) {
        AssetDto dto = new AssetDto();
        dto.setAssetId(asset.getAssetId());
        if (asset.getFolder() != null) {
            dto.setFolderId(asset.getFolder().getFolderId());
            dto.setFolderPath(asset.getFolder().getPath());
        }
        dto.setFileName(asset.getFileName());
        dto.setFileSize(asset.getFileSize());
        dto.setPixelWidth(asset.getPixelWidth());
        dto.setPixelHeight(asset.getPixelHeight());
        dto.setThumbnailPixelWidth(asset.getThumbnailPixelWidth());
        dto.setThumbnailPixelHeight(asset.getThumbnailPixelHeight());
        dto.setImageRotation(asset.getImageRotation());
        dto.setThumbnailCreationDateTime(asset.getThumbnailCreationDateTime());
        dto.setHash(asset.getHash());
        dto.setFileCreationDateTime(asset.getFileCreationDateTime());
        dto.setFileModificationDateTime(asset.getFileModificationDateTime());
        dto.setThumbnailUrl("/api/assets/" + asset.getAssetId() + "/thumbnail");
        dto.setImageUrl("/api/assets/" + asset.getAssetId() + "/image");
        dto.setRating(asset.getRating());
        return dto;
    }
}
