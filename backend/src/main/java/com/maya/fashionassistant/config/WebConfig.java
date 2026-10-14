package com.maya.fashionassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the built React app (copied into {@code classpath:/static/} at build time)
 * and falls back to {@code index.html} for client-side routes, so deep links such as
 * {@code /wardrobe} work when the app is opened directly. API paths are left alone.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] BACKEND_PREFIXES = {
            "api/", "api-docs", "swagger-ui", "v3/api-docs", "error"
    };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        for (String prefix : BACKEND_PREFIXES) {
                            if (resourcePath.startsWith(prefix)) {
                                return null;
                            }
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
