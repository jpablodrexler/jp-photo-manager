package com.jpablodrexler.photomanager.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "asset_audio")
@Data
@NoArgsConstructor
public class AssetAudioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", unique = true, nullable = false)
    private AssetEntity asset;

    @Column(name = "title")
    private String title;

    @Column(name = "artist")
    private String artist;

    @Column(name = "album")
    private String album;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "bitrate_kbps")
    private Integer bitrateKbps;

    @Column(name = "sample_rate_hz")
    private Integer sampleRateHz;
}
