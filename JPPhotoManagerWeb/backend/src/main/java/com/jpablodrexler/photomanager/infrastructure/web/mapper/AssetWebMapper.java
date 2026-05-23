package com.jpablodrexler.photomanager.infrastructure.web.mapper;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.TimelineGroup;
import com.jpablodrexler.photomanager.infrastructure.web.dto.AssetDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.TimelineGroupDto;
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
    AssetDto toDto(Asset asset);

    default TimelineGroupDto toTimelineGroupDto(TimelineGroup group) {
        List<AssetDto> assetDtos = group.getAssets().stream().map(this::toDto).toList();
        return new TimelineGroupDto(group.getLocalDate(), group.getLabel(), assetDtos);
    }

    @Named("tagsSetToList")
    default List<String> tagsSetToList(Set<String> tags) {
        if (tags == null) return Collections.emptyList();
        List<String> list = new ArrayList<>(tags);
        Collections.sort(list);
        return list;
    }
}
