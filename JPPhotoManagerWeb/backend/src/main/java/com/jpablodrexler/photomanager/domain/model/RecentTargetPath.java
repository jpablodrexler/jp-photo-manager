package com.jpablodrexler.photomanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTargetPath {

    private Long id;
    private String path;

    public RecentTargetPath(String path) {
        this.path = path;
    }
}
