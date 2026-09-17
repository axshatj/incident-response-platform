package com.irp.incident.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irp.incident.security.AuthFilter;
import com.irp.incident.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityFilterConfig {

    @Bean
    @ConditionalOnProperty(name = "irp.auth.enabled", havingValue = "true", matchIfMissing = true)
    FilterRegistrationBean<AuthFilter> authFilter(ObjectMapper mapper) {
        FilterRegistrationBean<AuthFilter> bean = new FilterRegistrationBean<>(new AuthFilter(mapper));
        bean.addUrlPatterns("/api/*");
        bean.setName("irpAuthFilter");
        bean.setOrder(1);
        return bean;
    }

    @Bean
    @ConditionalOnProperty(name = "irp.ratelimit.enabled", havingValue = "true", matchIfMissing = true)
    FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            ObjectMapper mapper,
            @Value("${irp.ratelimit.requests-per-minute:120}") int maxPerMinute) {
        FilterRegistrationBean<RateLimitFilter> bean =
                new FilterRegistrationBean<>(new RateLimitFilter(mapper, maxPerMinute));
        bean.addUrlPatterns("/api/*");
        bean.setName("irpRateLimitFilter");
        bean.setOrder(0);
        return bean;
    }
}
