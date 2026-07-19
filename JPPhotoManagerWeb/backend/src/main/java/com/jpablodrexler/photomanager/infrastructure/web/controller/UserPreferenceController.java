package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.model.UserPreference;
import com.jpablodrexler.photomanager.domain.port.in.preference.GetUserPreferenceUseCase;
import com.jpablodrexler.photomanager.domain.port.in.preference.SaveUserPreferenceUseCase;
import com.jpablodrexler.photomanager.domain.port.in.user.GetCurrentUserUseCase;
import com.jpablodrexler.photomanager.infrastructure.web.dto.shared.UserPreferenceDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Preferences", description = "User UI preferences")
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final GetUserPreferenceUseCase getUserPreferenceUseCase;
    private final SaveUserPreferenceUseCase saveUserPreferenceUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    @Operation(summary = "Get current user preference")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User preference"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<UserPreferenceDto> getPreference() {
        User user = resolveUser();
        UserPreference pref = getUserPreferenceUseCase.execute(user.getId());
        return ResponseEntity.ok(new UserPreferenceDto(pref.getThemeMode()));
    }

    @Operation(summary = "Save current user preference")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preference saved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping
    public ResponseEntity<Void> savePreference(@RequestBody UserPreferenceDto dto) {
        User user = resolveUser();
        saveUserPreferenceUseCase.execute(user.getId(), dto.themeMode());
        return ResponseEntity.ok().build();
    }

    private User resolveUser() {
        return getCurrentUserUseCase.execute();
    }
}
