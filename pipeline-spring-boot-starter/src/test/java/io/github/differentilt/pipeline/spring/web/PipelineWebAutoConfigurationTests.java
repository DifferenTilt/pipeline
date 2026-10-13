package io.github.differentilt.pipeline.spring.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.differentilt.pipeline.exception.BusinessRuleException;
import io.github.differentilt.pipeline.spring.PipelineAutoConfiguration;
import io.github.differentilt.pipeline.spring.PipelineConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

class PipelineWebAutoConfigurationTests {

    private static final AutoConfigurations AUTO_CONFIGURATIONS = AutoConfigurations.of(
            PipelineAutoConfiguration.class, PipelineWebAutoConfiguration.class);

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AUTO_CONFIGURATIONS);

    @Test
    void handlerIsNotRegisteredInANonWebContext() {
        new ApplicationContextRunner().withConfiguration(AUTO_CONFIGURATIONS).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(PipelineExceptionHandler.class);
            assertThat(context).doesNotHaveBean(PipelineWebProperties.class);
        });
    }

    @Test
    void handlerIsRegisteredInAServletWebContext() {
        webContextRunner.run(context -> assertThat(context).hasSingleBean(PipelineExceptionHandler.class));
        webContextRunner.withPropertyValues("pipeline.web.exception-handler.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(PipelineExceptionHandler.class));
    }

    @Test
    void handlerIsNotRegisteredWhenDisabled() {
        webContextRunner.withPropertyValues("pipeline.web.exception-handler.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(PipelineExceptionHandler.class);
                });
    }

    @Test
    void projectSubclassReplacesTheDefaultHandler() {
        webContextRunner.withUserConfiguration(CustomHandlerConfig.class).run(context -> {
            assertThat(context).hasSingleBean(PipelineExceptionHandler.class);
            assertThat(context.getBean(PipelineExceptionHandler.class)).isInstanceOf(CustomHandler.class);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 399, 600, -1})
    void statusOutsideErrorRangePreventsStartup(int status) {
        webContextRunner.withPropertyValues("pipeline.web.status-mapping.CONFLICT=" + status)
                .run(context -> assertThat(context).hasFailed().getFailure()
                        .hasRootCauseInstanceOf(PipelineConfigurationException.class)
                        .rootCause()
                        .hasMessageContaining("CONFLICT")
                        .hasMessageContaining(String.valueOf(status)));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 422, 599})
    void statusInsideErrorRangeIsAccepted(int status) {
        webContextRunner.withPropertyValues("pipeline.web.status-mapping.CONFLICT=" + status)
                .run(context -> assertThat(context).hasNotFailed());
    }

    static class CustomHandler extends PipelineExceptionHandler {

        CustomHandler(PipelineWebProperties properties) {
            super(properties);
        }

        @Override
        protected ProblemDetail businessProblem(BusinessRuleException e) {
            ProblemDetail problem = super.businessProblem(e);
            problem.setProperty("custom", true);
            return problem;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomHandlerConfig {

        @Bean
        CustomHandler customHandler(PipelineWebProperties properties) {
            return new CustomHandler(properties);
        }
    }
}
