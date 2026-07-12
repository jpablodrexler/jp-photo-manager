package com.jpablodrexler.photomanager.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sync_assets_directories_definitions")
@Data
@NoArgsConstructor
public class SyncAssetsDirectoriesDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_directory", nullable = false)
    private String sourceDirectory;

    @Column(name = "destination_directory", nullable = false)
    private String destinationDirectory;

    @Column(name = "include_sub_folders")
    private boolean includeSubFolders;

    @Column(name = "delete_assets_not_in_source")
    private boolean deleteAssetsNotInSource;

    @Column(name = "sort_order")
    private int order;
}
