package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AssetDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssetWebMapper {

    @Mapping(source = "folder.folderId", target = "folderId")
    @Mapping(source = "folder.path", target = "folderPath")
    @Mapping(target = "thumbnailUrl",
             expression = "java(\"/api/assets/\" + asset.getAssetId() + \"/thumbnail\")")
    @Mapping(target = "imageUrl",
             expression = "java(\"/api/assets/\" + asset.getAssetId() + \"/image\")")
    AssetDto toDto(Asset asset);
}
