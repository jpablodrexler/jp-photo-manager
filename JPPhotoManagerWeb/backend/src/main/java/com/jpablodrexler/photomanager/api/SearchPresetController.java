package com.jpablodrexler.photomanager.api;

import com.jpablodrexler.photomanager.api.dto.CreatePresetRequest;
import com.jpablodrexler.photomanager.api.dto.SearchPresetDto;
import com.jpablodrexler.photomanager.application.PhotoManagerFacade;
import com.jpablodrexler.photomanager.domain.entity.User;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search-presets")
@RequiredArgsConstructor
public class SearchPresetController {

    private final PhotoManagerFacade facade;
    private final UserRepository userRepository;

    @GetMapping
    public List<SearchPresetDto> list() {
        return facade.listSearchPresets(resolveUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchPresetDto create(@Valid @RequestBody CreatePresetRequest body) {
        return facade.saveSearchPreset(resolveUserId(), body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        facade.deleteSearchPreset(resolveUserId(), id);
    }

    private UUID resolveUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + username));
    }
}
