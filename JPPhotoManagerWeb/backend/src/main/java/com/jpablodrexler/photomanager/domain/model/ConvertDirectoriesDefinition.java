package com.jpablodrexler.photomanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertDirectoriesDefinition {

    private Long id;
    private String sourceDirectory;
    private String destinationDirectory;
    private boolean includeSubFolders;
    private boolean deleteAssetsNotInSource;
    private int order;
}
