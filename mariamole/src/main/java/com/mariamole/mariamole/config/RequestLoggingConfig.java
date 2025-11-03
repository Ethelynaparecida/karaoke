package com.mariamole.mariamole.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000); 
        filter.setIncludeHeaders(false);
        filter.setBeforeMessagePrefix("REQ INICIO: [");
        filter.setAfterMessagePrefix("REQ FIM: [");
        
        return filter;
    }
}