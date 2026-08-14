package com.jpablodrexler.photomanager;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class PhotoManagerApplicationTest {

    @Test
    void main_delegatesToSpringApplicationRun() {
        String[] args = {"--spring.profiles.active=test"};
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(PhotoManagerApplication.class, args)).thenReturn(context);

            PhotoManagerApplication.main(args);

            mocked.verify(() -> SpringApplication.run(PhotoManagerApplication.class, args));
        }
    }
}
