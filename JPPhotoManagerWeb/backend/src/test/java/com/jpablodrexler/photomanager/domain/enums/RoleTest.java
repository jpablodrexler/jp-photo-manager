package com.jpablodrexler.photomanager.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void values_returnsBothRoles() {
        assertThat(Role.values()).containsExactly(Role.ADMIN, Role.VIEWER);
    }

    @Test
    void valueOf_admin_returnsAdmin() {
        assertThat(Role.valueOf("ADMIN")).isEqualTo(Role.ADMIN);
    }

    @Test
    void valueOf_viewer_returnsViewer() {
        assertThat(Role.valueOf("VIEWER")).isEqualTo(Role.VIEWER);
    }
}
