package org.nr31.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nr31.backend.exception.JsonErrorReportValve;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer(ObjectMapper objectMapper) {
        return factory -> factory.addContextCustomizers(context ->
                context.getParent().getPipeline().addValve(new JsonErrorReportValve(objectMapper)));
    }
}
