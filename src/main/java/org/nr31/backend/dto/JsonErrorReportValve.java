package org.nr31.backend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JsonErrorReportValve extends ErrorReportValve {

    private final ObjectMapper objectMapper;

    public JsonErrorReportValve(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        if (!response.setErrorReported()) {
            return;
        }

        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, String> map = new HashMap<>();
            map.put("message", "Malformed URL or invalid characters in the request target.");
            map.put("timestamp", LocalDateTime.now().toString());

            objectMapper.writeValue(response.getWriter(), map);
            response.getWriter().flush();
        } catch (IOException e) {
            log.trace("IO error while writing tomcat error response");
        }
    }
}
