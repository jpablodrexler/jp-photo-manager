package com.jpablodrexler.photomanager.domain.entity;

import com.jpablodrexler.photomanager.domain.enums.ImageRotation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "assets", indexes = @Index(name = "ix_assets_folder_id", columnList = "folder_id"))
@Data
@NoArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id", columnDefinition = "INTEGER")
    private Long assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false, columnDefinition = "INTEGER")
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

    @Transient
    public String getThumbnailBlobName() {
        return assetId + ".bin";
    }

    @Transient
    public String getFullPath() {
        return folder != null ? folder.getPath() + "/" + fileName : fileName;
    }
}
