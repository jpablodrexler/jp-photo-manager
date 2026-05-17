package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {

    User toDomain(UserEntity entity);

    UserEntity toEntity(User domain);

    /**
     * Creates a minimal {@link UserEntity} containing only the primary key, suitable for use as a
     * JPA foreign-key reference (e.g. the {@code user} field on {@code RefreshTokenEntity}).
     * <p>
     * Use this instead of {@link #toEntity} when writing an owning-side association: JPA only needs
     * the {@code id} to populate the FK column and must not attempt to merge or update the referenced
     * {@code UserEntity} row with potentially stale field values from the domain object.
     */
    @Named("toUserEntityRef")
    default UserEntity toEntityRef(User domain) {
        if (domain == null) return null;
        UserEntity ref = new UserEntity();
        ref.setId(domain.getId());
        return ref;
    }
}
