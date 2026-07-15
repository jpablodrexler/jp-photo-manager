package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.FolderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface FolderEntityMapper {

    Folder toDomain(FolderEntity entity);

    FolderEntity toEntity(Folder domain);

    /**
     * Creates a minimal {@link FolderEntity} containing only the primary key, suitable for use as a
     * JPA foreign-key reference (e.g. the {@code folder} field on {@code AssetEntity}).
     * <p>
     * Use this instead of {@link #toEntity} when writing an owning-side association: JPA only needs
     * the {@code folderId} to populate the FK column and must not attempt to merge or update the
     * referenced {@code FolderEntity} row with potentially stale field values from the domain object.
     */
    @Named("toFolderEntityRef")
    default FolderEntity toEntityRef(Folder domain) {
        if (domain == null) return null;
        FolderEntity ref = new FolderEntity();
        ref.setFolderId(domain.getFolderId());
        return ref;
    }
}
