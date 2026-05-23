package com.jpablodrexler.photomanager.infrastructure.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthIndicatorTest {

    @Mock
    DataSource dataSource;

    @InjectMocks
    DatabaseHealthIndicator sut;

    @Test
    void health_selectSucceeds_returnsUp() throws Exception {
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery("SELECT 1")).thenReturn(rs);
        when(rs.next()).thenReturn(true);

        Health result = sut.health();

        assertThat(result.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void health_datasourceThrows_returnsDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        Health result = sut.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
    }
}
