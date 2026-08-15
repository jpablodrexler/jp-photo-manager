package com.jpablodrexler.photomanager.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;

class FolderTest {

    @Test
    void getName_normalPath_returnsLastSegment() {
        Folder sut = Folder.builder().path("/photos/vacation").build();

        assertThat(sut.getName()).isEqualTo("vacation");
    }

    @Test
    void getName_nullPath_returnsEmptyString() {
        Folder sut = Folder.builder().path(null).build();

        assertThat(sut.getName()).isEmpty();
    }

    @Test
    void getName_blankPath_returnsEmptyString() {
        Folder sut = Folder.builder().path("   ").build();

        assertThat(sut.getName()).isEmpty();
    }

    @Test
    void getParentPath_normalPath_returnsParentPath() {
        Folder sut = Folder.builder().path("/photos/vacation").build();

        assertThat(sut.getParentPath()).isEqualTo("/photos");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void getParentPath_windowsStylePath_returnsForwardSlashParentPath() {
        Folder sut = Folder.builder().path("C:\\photos\\vacation").build();

        assertThat(sut.getParentPath()).isEqualTo("C:/photos");
    }

    @Test
    void getParentPath_nullPath_returnsNull() {
        Folder sut = Folder.builder().path(null).build();

        assertThat(sut.getParentPath()).isNull();
    }

    @Test
    void getParentPath_blankPath_returnsNull() {
        Folder sut = Folder.builder().path("").build();

        assertThat(sut.getParentPath()).isNull();
    }

    @Test
    void getParentPath_rootPath_returnsNull() {
        Folder sut = Folder.builder().path("photos").build();

        assertThat(sut.getParentPath()).isNull();
    }

    @Test
    void isParentOf_directChildForwardSlash_returnsTrue() {
        Folder sut = Folder.builder().path("/photos").build();
        Folder other = Folder.builder().path("/photos/vacation").build();

        assertThat(sut.isParentOf(other)).isTrue();
    }

    @Test
    void isParentOf_directChildBackslash_returnsTrue() {
        Folder sut = Folder.builder().path("C:/photos").build();
        Folder other = Folder.builder().path("C:/photos\\vacation").build();

        assertThat(sut.isParentOf(other)).isTrue();
    }

    @Test
    void isParentOf_unrelatedFolder_returnsFalse() {
        Folder sut = Folder.builder().path("/photos").build();
        Folder other = Folder.builder().path("/music/vacation").build();

        assertThat(sut.isParentOf(other)).isFalse();
    }

    @Test
    void isParentOf_nullOther_returnsFalse() {
        Folder sut = Folder.builder().path("/photos").build();

        assertThat(sut.isParentOf(null)).isFalse();
    }

    @Test
    void isParentOf_otherWithNullPath_returnsFalse() {
        Folder sut = Folder.builder().path("/photos").build();
        Folder other = Folder.builder().path(null).build();

        assertThat(sut.isParentOf(other)).isFalse();
    }
}
