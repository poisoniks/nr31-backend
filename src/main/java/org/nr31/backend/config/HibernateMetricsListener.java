package org.nr31.backend.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.nr31.backend.dto.admin.AppConfigDto;
import org.nr31.backend.event.AppConfigUpdatedEvent;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.service.AppConfigService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Slf4j
@Component
@RequiredArgsConstructor
public class HibernateMetricsListener {

    private final EntityManagerFactory entityManagerFactory;
    private final AppConfigService appConfigService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        updateHibernateStatisticsState();
    }

    @EventListener(AppConfigUpdatedEvent.class)
    public void onAppConfigUpdated(AppConfigUpdatedEvent event) {
        if (AppConfigKey.FEATURE_SWITCHES.getKey().equals(event.getAppConfig().getConfigKey())) {
            updateHibernateStatisticsState();
        }
    }

    private void updateHibernateStatisticsState() {
        boolean enabled = isHibernateMetricsEnabled();
        try {
            SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            if (sessionFactory != null) {
                sessionFactory.getStatistics().setStatisticsEnabled(enabled);
                log.info("Hibernate statistics enabled status set to: {}", enabled);
            } else {
                log.warn("Could not unwrap SessionFactory from EntityManagerFactory. Hibernate statistics not toggled.");
            }
        } catch (Exception e) {
            log.error("Failed to update Hibernate statistics enabled state", e);
        }
    }

    private boolean isHibernateMetricsEnabled() {
        try {
            AppConfigDto config = appConfigService.getConfig(AppConfigKey.FEATURE_SWITCHES);
            JsonNode configValue = config.getConfigValue();
            if (configValue != null && configValue.isArray()) {
                for (JsonNode element : configValue) {
                    if (element.has("name") && "hibernate_metrics".equals(element.get("name").asString())) {
                        JsonNode enabledNode = element.get("enabled");
                        if (enabledNode != null) {
                            return enabledNode.asBoolean();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to check hibernate_metrics feature switch: {}", e.getMessage());
        }
        return false;
    }
}
