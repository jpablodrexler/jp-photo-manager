package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.UserSummary;
import com.jpablodrexler.photomanager.domain.port.in.user.CreateUserUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.DeleteUserUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.ListUsersUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.UpdatePasswordUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.CreateUserRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.UpdatePasswordRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "User Administration", description = "Admin-only user management")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final ListUsersUseCase listUsersUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    @Operation(summary = "List all users")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User list"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    @GetMapping
    public List<UserSummary> listUsers() {
        return listUsersUseCase.execute();
    }

    @Operation(summary = "Create a new user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    @PostMapping
    public ResponseEntity<UserSummary> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        UserSummary created = createUserUseCase.execute(request.username(), request.password(), "USER");
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Change a user's password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable UUID id,
                                               @Valid @RequestBody UpdatePasswordRequestDto request) {
        updatePasswordUseCase.execute(id, request.password());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete a user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        deleteUserUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
