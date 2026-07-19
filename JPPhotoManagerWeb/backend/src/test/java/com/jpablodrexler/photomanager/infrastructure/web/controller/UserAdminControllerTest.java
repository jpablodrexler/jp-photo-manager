package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.UserSummary;
import com.jpablodrexler.photomanager.domain.port.in.user.CreateUserUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.DeleteUserUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.ListUsersUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.UpdatePasswordUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.CreateUserRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.UpdatePasswordRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.UserSummaryResponseDto;
import com.jpablodrexler.photomanager.infrastructure.web.mapper.UserAdminWebMapper;
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

import static org.mockito.ArgumentMatchers.*;
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
    ListUsersUseCase listUsersUseCase;
    @MockitoBean
    CreateUserUseCase createUserUseCase;
    @MockitoBean
    UpdatePasswordUseCase updatePasswordUseCase;
    @MockitoBean
    DeleteUserUseCase deleteUserUseCase;
    @MockitoBean
    UserAdminWebMapper userAdminWebMapper;

    // --- GET /api/admin/users ---

    @Test
    void listUsers_returnsAllUsers_200() throws Exception {
        UUID id = UUID.randomUUID();
        UserSummary user = new UserSummary(id, "alice", Instant.parse("2025-01-01T00:00:00Z"));
        UserSummaryResponseDto dto = new UserSummaryResponseDto(id, "alice", Instant.parse("2025-01-01T00:00:00Z"));
        when(listUsersUseCase.execute()).thenReturn(List.of(user));
        when(userAdminWebMapper.toDtoList(List.of(user))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].id").value(id.toString()));
    }

    @Test
    void listUsers_emptyList_returns200WithEmptyArray() throws Exception {
        when(listUsersUseCase.execute()).thenReturn(List.of());
        when(userAdminWebMapper.toDtoList(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- POST /api/admin/users ---

    @Test
    void createUser_validRequest_returns201WithUserSummary() throws Exception {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        UserSummary created = new UserSummary(id, "bob", createdAt);
        UserSummaryResponseDto dto = new UserSummaryResponseDto(id, "bob", createdAt);
        when(createUserUseCase.execute("bob", "secret123", "USER")).thenReturn(created);
        when(userAdminWebMapper.toDto(created)).thenReturn(dto);

        CreateUserRequestDto request = new CreateUserRequestDto("bob", "secret123");
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void createUser_blankUsername_returns400() throws Exception {
        CreateUserRequestDto request = new CreateUserRequestDto("", "secret123");
        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- PATCH /api/admin/users/{id}/password ---

    @Test
    void updatePassword_validRequest_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(updatePasswordUseCase).execute(eq(id), eq("newpass"));

        UpdatePasswordRequestDto request = new UpdatePasswordRequestDto("newpass");
        mockMvc.perform(patch("/api/admin/users/" + id + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(updatePasswordUseCase).execute(id, "newpass");
    }

    // --- DELETE /api/admin/users/{id} ---

    @Test
    void deleteUser_existingUser_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(deleteUserUseCase).execute(id);

        mockMvc.perform(delete("/api/admin/users/" + id))
                .andExpect(status().isNoContent());

        verify(deleteUserUseCase).execute(id);
    }
}
