package com.jpablodrexler.photomanager.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sync_assets_directories_definitions")
@Data
@NoArgsConstructor
public class SyncAssetsDirectoriesDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "source_directory", nullable = false)
    private String sourceDirectory;

    @NotBlank
    @Column(name = "destination_directory", nullable = false)
    private String destinationDirectory;

    @Column(name = "include_sub_folders")
    private boolean includeSubFolders;

    @Column(name = "delete_assets_not_in_source")
    private boolean deleteAssetsNotInSource;

    @Column(name = "sort_order")
    private int order;
}
