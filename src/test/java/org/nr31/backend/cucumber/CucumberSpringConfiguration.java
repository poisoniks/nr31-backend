package org.nr31.backend.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.context.annotation.Import;

import java.util.Collections;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(JimfsTestConfig.class)
public class CucumberSpringConfiguration {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))
            .withCommand("postgres", "-c", "fsync=off", "-c", "full_page_writes=off");

    static final org.testcontainers.containers.GenericContainer<?> mailpit = new org.testcontainers.containers.GenericContainer<>("axllent/mailpit:latest")
            .withExposedPorts(1025, 8025);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        mailpit.start();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
        System.setProperty("mailpit.http.port", String.valueOf(mailpit.getMappedPort(8025)));
        System.setProperty("mailpit.host", mailpit.getHost());
    }
}
