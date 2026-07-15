package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.CropRegion;
import com.jpablodrexler.photomanager.domain.model.RenamePreview;
import com.jpablodrexler.photomanager.domain.model.TimelineGroup;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.CropAssetRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.AssetResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.ExifMetadataResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.RenamePreviewResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.TimelineGroupResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface AssetWebMapper {

    @Mapping(source = "folder.folderId", target = "folderId")
    @Mapping(source = "folder.path", target = "folderPath")
    @Mapping(target = "thumbnailUrl",
             expression = "java(\"/api/assets/\" + asset.getAssetId() + \"/thumbnail\")")
    @Mapping(target = "imageUrl",
             expression = "java(\"/api/assets/\" + asset.getAssetId() + \"/image\")")
    @Mapping(source = "tags", target = "tags", qualifiedByName = "tagsSetToList")
    AssetResponseDto toDto(Asset asset);

    ExifMetadataResponseDto toDto(AssetExif exif);

    RenamePreviewResponseDto toDto(RenamePreview preview);

    CropRegion toDomain(CropAssetRequestDto dto);

    default TimelineGroupResponseDto toTimelineGroupDto(TimelineGroup group) {
        List<AssetResponseDto> assetDtos = group.getAssets().stream().map(this::toDto).toList();
        return new TimelineGroupResponseDto(group.getLocalDate(), group.getLabel(), assetDtos);
    }

    @Named("tagsSetToList")
    default List<String> tagsSetToList(Set<String> tags) {
        if (tags == null) return Collections.emptyList();
        List<String> list = new ArrayList<>(tags);
        Collections.sort(list);
        return list;
    }
}
