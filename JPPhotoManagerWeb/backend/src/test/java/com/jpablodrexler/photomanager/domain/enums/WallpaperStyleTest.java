package com.jpablodrexler.photomanager.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WallpaperStyleTest {

    @Test
    void values_returnsAllSixStyles() {
        assertThat(WallpaperStyle.values()).containsExactly(
            WallpaperStyle.CENTER,
            WallpaperStyle.FILL,
            WallpaperStyle.FIT,
            WallpaperStyle.SPAN,
            WallpaperStyle.STRETCH,
            WallpaperStyle.TILE
        );
    }

    @Test
    void valueOf_eachName_returnsMatchingConstant() {
        for (WallpaperStyle style : WallpaperStyle.values()) {
            assertThat(WallpaperStyle.valueOf(style.name())).isEqualTo(style);
        }
    }
}
