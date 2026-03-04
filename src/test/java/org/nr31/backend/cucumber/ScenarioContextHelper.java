package org.nr31.backend.cucumber;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ScenarioContextHelper {
    private final ThreadLocal<Map<String, Object>> scenarioContextValues = new ThreadLocal<>();

    public void initialize() {
        scenarioContextValues.set(new HashMap<>());
    }

    public void release() {
        scenarioContextValues.remove();
    }

    public <T> void addValue(String key, T value) {
        Map<String, Object> map = scenarioContextValues.get();
        map.put(key, value);
    }

    public <T> T getValue(String key) {
        return (T) scenarioContextValues.get().get(key);
    }
}
