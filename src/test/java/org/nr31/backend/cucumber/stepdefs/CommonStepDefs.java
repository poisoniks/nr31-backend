package org.nr31.backend.cucumber.stepdefs;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import io.cucumber.datatable.DataTable;
import org.nr31.backend.cucumber.ScenarioContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CommonStepDefs {

    @LocalServerPort
    protected int port;

    @Autowired
    protected ScenarioContextHelper contextHelper;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    protected String dataTableToJson(DataTable dataTable) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        Map<String, String> map = dataTable.asMap(String.class, String.class);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            ObjectNode current = root;
            for (int i = 0; i < parts.length - 1; i++) {
                if (!current.has(parts[i])) {
                    current.set(parts[i], objectMapper.createObjectNode());
                }
                current = (ObjectNode) current.get(parts[i]);
            }
            String key = parts[parts.length - 1];
            String value = entry.getValue();

            if (value == null || value.equals("null")) {
                current.set(key, objectMapper.nullNode());
            } else if ((value.startsWith("[") && value.endsWith("]"))
                    || (value.startsWith("{") && value.endsWith("}"))) {
                if (key.equals("configValue") || key.equals("configSchema")) {
                    current.put(key, value);
                } else {
                    current.set(key, objectMapper.readTree(value));
                }
            } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                current.put(key, Boolean.parseBoolean(value));
            } else if (value.matches("-?\\d+")) {
                current.put(key, Integer.parseInt(value));
            } else {
                current.put(key, value);
            }
        }
        return root.toString();
    }

    protected String resolveVariables(String text) {
        if (text == null)
            return null;
        String resolved = text;
        Matcher m = Pattern.compile("\\{([^}]+)}").matcher(text);
        while (m.find()) {
            String key = m.group(1);
            Object value = contextHelper.getValue(key);
            if (value != null) {
                resolved = resolved.replace("{" + key + "}", value.toString());
            }
        }
        return resolved;
    }

    protected HttpResponse<String> makeApiCall(String method, String url, String body) throws Exception {
        String resolvedUrl = resolveVariables(url);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + resolvedUrl))
                .header("Content-Type", "application/json");

        String token = contextHelper.getValue("jwt_token");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        if (body != null && !body.isEmpty()) {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
