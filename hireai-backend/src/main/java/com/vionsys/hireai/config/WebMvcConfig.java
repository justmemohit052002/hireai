package com.vionsys.hireai.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.vionsys.hireai.security.resolver.CurrentUserArgumentResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Value("${photo.upload-dir:uploads/profile-photos}")
    private String photoUploadDir;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(photoUploadDir).toAbsolutePath().normalize();
        String uploadUri = uploadPath.toUri().toString();
        registry.addResourceHandler("/uploads/profile-photos/**")
                .addResourceLocations(uploadUri.endsWith("/") ? uploadUri : uploadUri + "/");
    }
}
