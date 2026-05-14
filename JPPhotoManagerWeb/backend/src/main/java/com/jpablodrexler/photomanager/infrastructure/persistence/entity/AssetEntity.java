package com.jpablodrexler.photomanager.infrastructure.persistence.entity;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "assets", indexes = @Index(name = "ix_assets_folder_id", columnList = "folder_id"))
@Data
@NoArgsConstructor
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private FolderEntity folder;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "pixel_width")
    private Integer pixelWidth;

    @Column(name = "pixel_height")
    private Integer pixelHeight;

    @Column(name = "thumbnail_pixel_width")
    private Integer thumbnailPixelWidth;

    @Column(name = "thumbnail_pixel_height")
    private Integer thumbnailPixelHeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_rotation")
    private ImageRotation imageRotation;

    @Column(name = "thumbnail_creation_date_time", nullable = false)
    private LocalDateTime thumbnailCreationDateTime;

    @Column(name = "hash", nullable = false)
    private String hash;

    @Column(name = "file_creation_date_time")
    private LocalDateTime fileCreationDateTime;

    @Column(name = "file_modification_date_time")
    private LocalDateTime fileModificationDateTime;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "rating", nullable = false)
    private int rating = 0;

    @Transient
    public String getThumbnailBlobName() {
        return assetId + ".bin";
    }

    @Transient
    public String getFullPath() {
        return folder != null ? folder.getPath() + "/" + fileName : fileName;
    }
}
