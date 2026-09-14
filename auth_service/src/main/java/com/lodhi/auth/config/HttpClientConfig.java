package com.lodhi.auth.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.lodhi.auth.utils.CorrelationIdUtils;

/**
 * Configuration for HTTP clients used for external API calls.
 * Configures timeouts to prevent hanging requests and automatically
 * propagates correlation IDs for distributed tracing.
 */
@Configuration
public class HttpClientConfig {

    /**
     * RestTemplate with configured timeouts and correlation ID propagation.
     * 
     * Timeouts:
     * - Connection timeout: 5 seconds (time to establish connection)
     * - Read timeout: 10 seconds (time to read response)
     * 
     * Correlation ID:
     * - Automatically adds X-Request-ID and X-Correlation-ID headers
     * - Enables end-to-end tracing across services
     * 
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 seconds
        factory.setReadTimeout(10000);     // 10 seconds
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Add interceptor to propagate correlation ID
        restTemplate.setInterceptors(Collections.singletonList(correlationIdInterceptor()));
        
        return restTemplate;
    }

    /**
     * Interceptor that automatically adds correlation ID headers to outgoing HTTP requests.
     * This enables distributed tracing across multiple services.
     * 
     * @return ClientHttpRequestInterceptor that adds correlation ID headers
     */
    private ClientHttpRequestInterceptor correlationIdInterceptor() {
        return (request, body, execution) -> {
            String correlationId = CorrelationIdUtils.getCurrentCorrelationId();
            
            if (correlationId != null) {
                // Add correlation ID to outgoing request headers
                request.getHeaders().add("X-Request-ID", correlationId);
                request.getHeaders().add("X-Correlation-ID", correlationId);
            }
            
            return execution.execute(request, body);
        };
    }
}
