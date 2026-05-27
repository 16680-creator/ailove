package com.ailovedaily.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:/app/uploads}")
    private String uploadPath;

    @Value("${file.access.url:/uploads}")
    private String accessUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = Paths.get(uploadPath);
        if (!uploadRoot.isAbsolute()) {
            uploadRoot = Paths.get(System.getProperty("user.dir")).resolve(uploadRoot);
        }

        registry.addResourceHandler(accessUrl + "/**")
                .addResourceLocations(uploadRoot.toAbsolutePath().normalize().toUri().toString());
    }
}
