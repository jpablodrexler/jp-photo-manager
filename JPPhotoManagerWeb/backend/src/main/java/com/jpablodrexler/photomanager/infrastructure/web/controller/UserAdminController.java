package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.application.dto.UserSummary;
import com.jpablodrexler.photomanager.domain.port.in.user.CreateUserUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.DeleteUserUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.ListUsersUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.UpdatePasswordUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.CreateUserRequest;
import com.jpablodrexler.photomanager.infrastructure.web.UpdatePasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final ListUsersUseCase listUsersUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdatePasswordUseCase updatePasswordUseCase;
    private final DeleteUserUseCase deleteUserUseCase;

    @GetMapping
    public List<UserSummary> listUsers() {
        return listUsersUseCase.execute();
    }

    @PostMapping
    public ResponseEntity<UserSummary> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserSummary created = createUserUseCase.execute(request.username(), request.password(), "USER");
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable UUID id,
                                               @Valid @RequestBody UpdatePasswordRequest request) {
        updatePasswordUseCase.execute(id, request.password());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        deleteUserUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
