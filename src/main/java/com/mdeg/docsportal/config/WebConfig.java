package com.mdeg.docsportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve Bootstrap + Bootstrap Icons (extracted to static/webjars/)
        registry.addResourceHandler("/webjars/bootstrap/**")
            .addResourceLocations("classpath:/static/webjars/bootstrap/");
        registry.addResourceHandler("/webjars/bootstrap-icons/**")
            .addResourceLocations("classpath:/static/webjars/bootstrap-icons/");
    }
}
