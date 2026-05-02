package com.jpablodrexler.photomanager.config;

import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import com.jpablodrexler.photomanager.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final UserService userService;

    @EventListener(ApplicationReadyEvent.class)
    public void seedDefaultAdmin() {
        if (userRepository.count() == 0) {
            userService.register("admin", "admin");
            log.warn("*** Default admin user created (username: admin, password: admin). CHANGE THIS PASSWORD IMMEDIATELY via the User Administration page. ***");
        }
    }
}
