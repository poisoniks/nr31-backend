package org.nr31.backend.config;

import lombok.RequiredArgsConstructor;
import org.nr31.backend.interceptor.FeatureSwitchInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FeatureSwitchInterceptor featureSwitchInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(featureSwitchInterceptor);
    }
}
