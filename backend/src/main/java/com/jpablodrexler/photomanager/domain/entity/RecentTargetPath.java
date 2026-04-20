package com.jpablodrexler.photomanager.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recent_target_paths")
@Data
@NoArgsConstructor
public class RecentTargetPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "path", nullable = false)
    private String path;

    public RecentTargetPath(String path) {
        this.path = path;
    }
}
