package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.Before;
import org.hibernate.SessionFactory;
import org.nr31.backend.cucumber.DatabaseCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

public class DatabaseHooks {

    @Autowired
    private DatabaseCleanupService cleanupService;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private CacheManager cacheManager;

    @Before(order = 0)
    public void resetDatabase() {
        cleanupService.createSnapshot();
        cleanupService.resetDatabase();
        sessionFactory.getCache().evictAllRegions();
        cacheManager.resetCaches();
    }
}
