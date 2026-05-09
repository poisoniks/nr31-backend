package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.Before;
import jakarta.persistence.EntityManagerFactory;
import org.nr31.backend.cucumber.DatabaseCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

public class DatabaseHooks {

    @Autowired
    private DatabaseCleanupService cleanupService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private CacheManager cacheManager;

    @Before(order = 0)
    public void resetDatabase() {
        cleanupService.createSnapshot();
        cleanupService.resetDatabase();
        entityManagerFactory.getCache().evictAll();
        cacheManager.resetCaches();
    }
}
