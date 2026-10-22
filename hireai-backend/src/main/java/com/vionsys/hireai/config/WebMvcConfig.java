package com.vionsys.hireai.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.vionsys.hireai.candidate.storage.StorageProperties;
import com.vionsys.hireai.security.resolver.CurrentUserArgumentResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final ObjectProvider<StorageProperties> storagePropertiesProvider;

    public WebMvcConfig(
            CurrentUserArgumentResolver currentUserArgumentResolver,
            ObjectProvider<StorageProperties> storagePropertiesProvider) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
        this.storagePropertiesProvider = storagePropertiesProvider;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        StorageProperties storageProperties = storagePropertiesProvider.getIfAvailable();
        Path uploadPath = (storageProperties != null)
                ? storageProperties.getPhotoUploadPath()
                : StorageProperties.resolveBaseDirectory(null).resolve("profile-photos").normalize();
        String uploadUri = uploadPath.toUri().toString();
        registry.addResourceHandler("/uploads/profile-photos/**")
                .addResourceLocations(uploadUri.endsWith("/") ? uploadUri : uploadUri + "/");
    }
}
