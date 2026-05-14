package com.jpablodrexler.photomanager.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;

@Entity
@Table(name = "folders")
@Data
@NoArgsConstructor
public class FolderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "path", nullable = false)
    private String path;

    @Transient
    public String getName() {
        if (path == null || path.isBlank())
            return "";
        Path p = Paths.get(path);
        return p.getFileName() != null ? p.getFileName().toString() : path;
    }

    @Transient
    public String getParentPath() {
        if (path == null || path.isBlank())
            return null;
        Path p = Paths.get(path);
        return p.getParent() != null ? p.getParent().toString() : null;
    }
}
