package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.Before;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManagerFactory;

public class DatabaseHooks {

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Before(order = 0)
    public void resetDatabase() {
        flyway.clean();
        flyway.migrate();
        entityManagerFactory.getCache().evictAll();
    }
}
