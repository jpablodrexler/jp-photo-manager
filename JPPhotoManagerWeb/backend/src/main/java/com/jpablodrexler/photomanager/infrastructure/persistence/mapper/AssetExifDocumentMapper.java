package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.infrastructure.persistence.document.AssetExifDocument;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

@Mapper(componentModel = "spring")
public abstract class AssetExifDocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "location", ignore = true)
    public abstract AssetExifDocument toDocument(AssetExif assetExif);

    public abstract AssetExif toDomain(AssetExifDocument document);

    @AfterMapping
    protected void deriveLocation(AssetExif source, @MappingTarget AssetExifDocument target) {
        if (source.getGpsLatitude() != null && source.getGpsLongitude() != null) {
            target.setLocation(new GeoJsonPoint(source.getGpsLongitude(), source.getGpsLatitude()));
        }
    }
}
