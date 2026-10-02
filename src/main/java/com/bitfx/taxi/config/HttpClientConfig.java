package com.bitfx.taxi.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Clientes HTTP compartidos hacia proveedores externos de geo (routing/geocoding). Cada uno con su
 * propio timeout configurable, para poder distinguir "el proveedor tardo demasiado" de otros errores
 * y para no dejar una llamada colgada indefinidamente si el servicio publico esta lento/caido.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @Qualifier("routingRestTemplate")
    public RestTemplate routingRestTemplate(RestTemplateBuilder builder,
                                             @Value("${app.routing.http-timeout-ms}") long timeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Bean
    @Qualifier("geocodingRestTemplate")
    public RestTemplate geocodingRestTemplate(RestTemplateBuilder builder,
                                               @Value("${app.geocoding.http-timeout-ms}") long timeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
