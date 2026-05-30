package org.nr31.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] flywayLocations;

    @Value("${spring.flyway.cleanDisabled:true}")
    private boolean flywayCleanDisabled;

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations)
                .baselineOnMigrate(true)
                .cleanDisabled(flywayCleanDisabled)
                .load();
    }
}
