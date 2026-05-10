package com.jpablodrexler.photomanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.application.dto.UserSummary;
import com.jpablodrexler.photomanager.domain.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAdminController.class)
@ActiveProfiles("test")
class UserAdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserAdminService userAdminService;

    @Test
    void listUsers_returnsUsers() throws Exception {
        UUID id = UUID.randomUUID();
        when(userAdminService.listUsers()).thenReturn(List.of(
                new UserSummary(id, "alice", Instant.parse("2024-01-01T00:00:00Z"))
        ));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    void createUser_validRequest_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(userAdminService.createUser("bob", "pass123"))
                .thenReturn(new UserSummary(id, "bob", Instant.now()));

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","password":"pass123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    void updatePassword_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(userAdminService).updatePassword(eq(id), anyString());

        mockMvc.perform(patch("/api/admin/users/" + id + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"newpass"}
                                """))
                .andExpect(status().isOk());

        verify(userAdminService).updatePassword(eq(id), eq("newpass"));
    }

    @Test
    void deleteUser_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(userAdminService).deleteUser(id);

        mockMvc.perform(delete("/api/admin/users/" + id))
                .andExpect(status().isNoContent());

        verify(userAdminService).deleteUser(id);
    }
}
