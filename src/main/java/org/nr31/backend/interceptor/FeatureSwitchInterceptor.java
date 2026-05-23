package org.nr31.backend.interceptor;

import tools.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.annotation.FeatureSwitch;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.exception.FeatureDisabledException;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.service.AppConfigService;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureSwitchInterceptor implements HandlerInterceptor {

    private final AppConfigService appConfigService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            FeatureSwitch featureSwitch = handlerMethod.getMethodAnnotation(FeatureSwitch.class);
            if (featureSwitch == null) {
                featureSwitch = handlerMethod.getBeanType().getAnnotation(FeatureSwitch.class);
            }

            if (featureSwitch != null) {
                String featureName = featureSwitch.value();
                if (!isFeatureEnabled(featureName)) {
                    log.debug("Endpoint access denied. Feature '{}' is disabled.", featureName);
                    throw new FeatureDisabledException("Feature '" + featureName + "' is disabled");
                }
            }
        }
        return true;
    }

    private boolean isFeatureEnabled(String featureName) {
        try {
            AppConfigDto config = appConfigService.getConfig(AppConfigKey.FEATURE_SWITCHES);
            JsonNode configNode = config.getConfigValue();

            if (configNode != null && configNode.isArray()) {
                for (JsonNode element : configNode) {
                    if (element.has("name") && featureName.equals(element.get("name").asText())) {
                        JsonNode enabledNode = element.get("enabled");
                        if (enabledNode != null) {
                            if (enabledNode.isBoolean()) {
                                return enabledNode.asBoolean();
                            } else if (enabledNode.isTextual()) {
                                return Boolean.parseBoolean(enabledNode.asText());
                            }
                        }
                    }
                }
            }
            return false;
        } catch (ElementNotFoundException e) {
            log.warn(
                    "Feature switches configuration 'feature_switches' not found! All feature-switched endpoints consider feature disabled.");
            return false;
        } catch (Exception e) {
            log.error("Failed to parse 'feature_switches' configuration. Features default to disabled.", e);
            return false;
        }
    }
}
