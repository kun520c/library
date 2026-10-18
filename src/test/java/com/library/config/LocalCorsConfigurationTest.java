package com.library.config;

import com.library.config.properties.CorsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class LocalCorsConfigurationTest {
    // Load tracked configuration only, never the developer's private application-local.yaml.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CorsConfiguration.class)
            .withInitializer(context -> {
                var loader = new YamlPropertySourceLoader();
                try {
                    var sources = context.getEnvironment().getPropertySources();
                    loader.load("local-example", new ClassPathResource("application-local.example.yaml"))
                            .forEach(sources::addLast);
                    loader.load("common", new ClassPathResource("application.yaml"))
                            .forEach(sources::addLast);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });

    @Test
    void localExamplePreservesEnvironmentOriginsInsteadOfOverridingThem() {
        runner.withPropertyValues("CORS_ALLOWED_ORIGINS=https://portfolio.example,http://localhost:6173")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CorsProperties.class).allowedOrigins())
                            .containsExactly("https://portfolio.example", "http://localhost:6173");
                });
    }

    @Test
    void originsRemainRequiredInsteadOfSilentlyEnablingLocalhost() {
        runner.withPropertyValues("CORS_ALLOWED_ORIGINS=").run(context ->
                assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    static class CorsConfiguration {
    }
}
