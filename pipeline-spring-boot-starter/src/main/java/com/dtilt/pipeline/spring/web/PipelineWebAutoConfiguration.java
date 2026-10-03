package com.dtilt.pipeline.spring.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ProblemDetail;

/**
 * Auto-configuration web: nelle applicazioni web servlet registra il
 * {@link PipelineExceptionHandler} di default, a meno che il progetto ne definisca uno proprio o
 * lo disattivi con {@code pipeline.web.exception-handler.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ProblemDetail.class)
@ConditionalOnProperty(name = "pipeline.web.exception-handler.enabled",
                       havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PipelineWebProperties.class)
public class PipelineWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PipelineExceptionHandler.class)
    PipelineExceptionHandler pipelineExceptionHandler(PipelineWebProperties properties) {
        return new PipelineExceptionHandler(properties);
    }
}
