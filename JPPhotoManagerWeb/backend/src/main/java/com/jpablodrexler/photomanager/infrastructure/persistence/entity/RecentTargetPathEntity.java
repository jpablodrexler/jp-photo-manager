package com.jpablodrexler.photomanager.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recent_target_paths")
@Data
@NoArgsConstructor
public class RecentTargetPathEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "path", nullable = false)
    private String path;

    public RecentTargetPathEntity(String path) {
        this.path = path;
    }
}
