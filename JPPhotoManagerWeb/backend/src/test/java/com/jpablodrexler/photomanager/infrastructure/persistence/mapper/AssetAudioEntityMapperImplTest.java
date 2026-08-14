package com.jpablodrexler.photomanager.infrastructure.persistence.mapper;

import com.jpablodrexler.photomanager.domain.model.AssetAudio;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetAudioEntity;
import com.jpablodrexler.photomanager.infrastructure.persistence.entity.AssetEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetAudioEntityMapperImplTest {

    private final AssetAudioEntityMapper sut = new AssetAudioEntityMapperImpl();

    @Test
    void toDomain_entityWithAsset_mapsAllFieldsIncludingAssetId() {
        AssetEntity asset = new AssetEntity();
        asset.setAssetId(10L);

        AssetAudioEntity entity = new AssetAudioEntity();
        entity.setAsset(asset);
        entity.setTitle("Song Title");
        entity.setArtist("Artist Name");
        entity.setAlbum("Album Name");
        entity.setDurationSeconds(240);
        entity.setBitrateKbps(320);
        entity.setSampleRateHz(44100);

        AssetAudio result = sut.toDomain(entity);

        assertThat(result.getAssetId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("Song Title");
        assertThat(result.getArtist()).isEqualTo("Artist Name");
        assertThat(result.getAlbum()).isEqualTo("Album Name");
        assertThat(result.getDurationSeconds()).isEqualTo(240);
        assertThat(result.getBitrateKbps()).isEqualTo(320);
        assertThat(result.getSampleRateHz()).isEqualTo(44100);
    }

    @Test
    void toDomain_entityWithNullAsset_leavesAssetIdNull() {
        AssetAudioEntity entity = new AssetAudioEntity();
        entity.setTitle("No Asset");

        AssetAudio result = sut.toDomain(entity);

        assertThat(result.getAssetId()).isNull();
        assertThat(result.getTitle()).isEqualTo("No Asset");
    }

    @Test
    void toDomain_nullEntity_returnsNull() {
        assertThat(sut.toDomain(null)).isNull();
    }

    @Test
    void updateEntityFromDomain_validDomain_updatesFieldsButNotIdOrAsset() {
        AssetEntity asset = new AssetEntity();
        asset.setAssetId(99L);
        AssetAudioEntity entity = new AssetAudioEntity();
        entity.setId(1L);
        entity.setAsset(asset);

        AssetAudio domain = AssetAudio.builder()
                .title("New Title")
                .artist("New Artist")
                .album("New Album")
                .durationSeconds(180)
                .bitrateKbps(256)
                .sampleRateHz(48000)
                .build();

        sut.updateEntityFromDomain(domain, entity);

        assertThat(entity.getTitle()).isEqualTo("New Title");
        assertThat(entity.getArtist()).isEqualTo("New Artist");
        assertThat(entity.getAlbum()).isEqualTo("New Album");
        assertThat(entity.getDurationSeconds()).isEqualTo(180);
        assertThat(entity.getBitrateKbps()).isEqualTo(256);
        assertThat(entity.getSampleRateHz()).isEqualTo(48000);
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getAsset()).isSameAs(asset);
    }

    @Test
    void updateEntityFromDomain_nullDomain_leavesEntityUnchanged() {
        AssetAudioEntity entity = new AssetAudioEntity();
        entity.setTitle("Untouched");

        sut.updateEntityFromDomain(null, entity);

        assertThat(entity.getTitle()).isEqualTo("Untouched");
    }
}
