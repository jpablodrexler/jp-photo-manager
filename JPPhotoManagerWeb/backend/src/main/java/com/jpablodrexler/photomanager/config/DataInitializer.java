package com.jpablodrexler.photomanager.config;

import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
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
            userRepository.findByUsername("admin").ifPresent(user -> {
                user.setRole("ADMIN");
                userRepository.save(user);
            });
            log.warn("*** Default admin user created. CHANGE THIS PASSWORD IMMEDIATELY via the User Administration page. ***");
        }
    }

}
