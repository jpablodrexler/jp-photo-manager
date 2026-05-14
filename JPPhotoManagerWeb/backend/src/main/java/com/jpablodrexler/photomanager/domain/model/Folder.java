package com.jpablodrexler.photomanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Folder {

    private Long folderId;
    private String path;

    public String getName() {
        if (path == null || path.isBlank())
            return "";
        Path p = Paths.get(path);
        return p.getFileName() != null ? p.getFileName().toString() : path;
    }

    public String getParentPath() {
        if (path == null || path.isBlank())
            return null;
        Path p = Paths.get(path);
        return p.getParent() != null ? p.getParent().toString() : null;
    }

    public boolean isParentOf(Folder other) {
        if (other == null || other.path == null)
            return false;
        return other.path.startsWith(this.path + "/") || other.path.startsWith(this.path + "\\");
    }
}
