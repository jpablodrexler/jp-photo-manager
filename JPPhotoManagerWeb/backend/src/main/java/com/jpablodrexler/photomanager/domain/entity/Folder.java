package com.jpablodrexler.photomanager.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Entity
@Table(name = "folders")
@Data
@NoArgsConstructor
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id", columnDefinition = "INTEGER")
    private Long folderId;

    @Column(name = "path", nullable = false)
    private String path;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Asset> assets;

    @Transient
    public String getName() {
        if (path == null || path.isBlank()) return "";
        Path p = Paths.get(path);
        return p.getFileName() != null ? p.getFileName().toString() : path;
    }

    @Transient
    public String getParentPath() {
        if (path == null || path.isBlank()) return null;
        Path p = Paths.get(path);
        return p.getParent() != null ? p.getParent().toString() : null;
    }

    public boolean isParentOf(Folder other) {
        if (other == null || other.path == null) return false;
        return other.path.startsWith(this.path + "/") || other.path.startsWith(this.path + "\\");
    }
}
