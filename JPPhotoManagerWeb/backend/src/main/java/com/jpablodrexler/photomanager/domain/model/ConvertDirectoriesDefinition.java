package com.jpablodrexler.photomanager.domain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertDirectoriesDefinition {

    private Long id;
    @NotBlank
    private String sourceDirectory;
    @NotBlank
    private String destinationDirectory;
    private boolean includeSubFolders;
    private boolean deleteAssetsNotInSource;
    private int order;
}
