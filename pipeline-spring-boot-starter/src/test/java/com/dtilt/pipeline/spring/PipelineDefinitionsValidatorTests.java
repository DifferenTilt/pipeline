package com.dtilt.pipeline.spring;

import static org.assertj.core.api.Assertions.assertThat;

import com.dtilt.pipeline.Pipeline;
import com.dtilt.pipeline.PipelineFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ExtendWith(OutputCaptureExtension.class)
class PipelineDefinitionsValidatorTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PipelineAutoConfiguration.class))
            .withUserConfiguration(PipelinesConfig.class);

    @Test
    void knownEntriesAreAccepted() {
        contextRunner.withPropertyValues("pipeline.disabled-steps=create-product.date-range,update-product.audit")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void unknownPipelinePreventsStartup() {
        contextRunner.withPropertyValues("pipeline.disabled-steps=delete-product.audit")
                .run(context -> assertThat(StartupFailures.rootCause(context))
                        .isInstanceOf(PipelineConfigurationException.class)
                        .hasMessageContaining("'delete-product.audit'")
                        .hasMessageContaining("pipeline 'delete-product' does not exist"));
    }

    @Test
    void unknownStepOfKnownPipelinePreventsStartup() {
        contextRunner.withPropertyValues("pipeline.disabled-steps=create-product.missing-step")
                .run(context -> assertThat(StartupFailures.rootCause(context))
                        .isInstanceOf(PipelineConfigurationException.class)
                        .hasMessageContaining("'create-product.missing-step'")
                        .hasMessageContaining("step 'missing-step' does not exist in pipeline 'create-product'"));
    }

    @Test
    void requiredStepCannotBeDisabled() {
        contextRunner.withPropertyValues("pipeline.disabled-steps=update-product.save")
                .run(context -> assertThat(StartupFailures.rootCause(context))
                        .isInstanceOf(PipelineConfigurationException.class)
                        .hasMessageContaining("'update-product.save'")
                        .hasMessageContaining(
                                "step 'save' is required in pipeline 'update-product' and cannot be disabled"));
    }

    @Test
    void requiredStepPreventsStartupEvenWhenFailOnUnknownStepsIsFalse() {
        contextRunner.withPropertyValues(
                        "pipeline.disabled-steps=update-product.save",
                        "pipeline.fail-on-unknown-steps=false")
                .run(context -> assertThat(StartupFailures.rootCause(context))
                        .isInstanceOf(PipelineConfigurationException.class)
                        .hasMessageContaining("step 'save' is required in pipeline 'update-product'"));
    }

    @Test
    void requiredStepIsReportedTogetherWithTheOtherProblems() {
        contextRunner.withPropertyValues(
                        "pipeline.disabled-steps=update-product.save,delete-product.audit,update-product.audit")
                .run(context -> {
                    Throwable failure = StartupFailures.rootCause(context);
                    assertThat(failure).isInstanceOf(PipelineConfigurationException.class);
                    String message = failure.getMessage();
                    assertThat(message)
                            .contains("step 'save' is required in pipeline 'update-product'")
                            .contains("pipeline 'delete-product' does not exist")
                            .doesNotContain("'update-product.audit'");
                    assertThat(message.lines()).hasSize(3);
                    assertThat(message.indexOf("is required")).isLessThan(message.indexOf("does not exist"));
                });
    }

    @Test
    void allProblemsAreReportedInTheSameMessage() {
        contextRunner.withUserConfiguration(DuplicateConfig.class)
                .withPropertyValues(
                        "pipeline.disabled-steps=delete-product.audit,create-product.missing-step,create-product.date-range")
                .run(context -> {
                    Throwable failure = StartupFailures.rootCause(context);
                    assertThat(failure).isInstanceOf(PipelineConfigurationException.class);
                    String message = failure.getMessage();
                    assertThat(message)
                            .contains("pipeline 'delete-product' does not exist")
                            .contains("step 'missing-step' does not exist in pipeline 'create-product'")
                            .contains("duplicate pipeline name 'update-product'")
                            .doesNotContain("'create-product.date-range'");
                    assertThat(message.lines()).hasSize(4);
                });
    }

    @Test
    void unknownEntriesAreOnlyLoggedWhenFailOnUnknownStepsIsFalse(CapturedOutput output) {
        contextRunner.withPropertyValues(
                        "pipeline.disabled-steps=delete-product.audit,create-product.missing-step",
                        "pipeline.fail-on-unknown-steps=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(output.getAll().lines().filter(line -> line.startsWith("WARN")))
                            .hasSize(2)
                            .anySatisfy(line -> assertThat(line)
                                    .contains("pipeline 'delete-product' does not exist"))
                            .anySatisfy(line -> assertThat(line)
                                    .contains("step 'missing-step' does not exist in pipeline 'create-product'"));
                });
    }

    @Test
    void duplicatePipelineNamesPreventStartupEvenWhenFailOnUnknownStepsIsFalse() {
        contextRunner.withUserConfiguration(DuplicateConfig.class)
                .withPropertyValues("pipeline.fail-on-unknown-steps=false")
                .run(context -> assertThat(StartupFailures.rootCause(context))
                        .isInstanceOf(PipelineConfigurationException.class)
                        .hasMessageContaining("duplicate pipeline name 'update-product'")
                        .hasMessageContaining("updateProductPipeline")
                        .hasMessageContaining("anotherUpdateProductPipeline"));
    }

    @Configuration(proxyBeanMethods = false)
    static class PipelinesConfig {

        @Bean
        Pipeline<List<String>> createProductPipeline(PipelineFactory factory) {
            return factory.<List<String>>pipeline("create-product")
                    .step("name-unique", trace -> trace.add("name-unique"))
                    .step("date-range", trace -> trace.add("date-range"))
                    .build();
        }

        @Bean
        Pipeline<List<String>> updateProductPipeline(PipelineFactory factory) {
            return factory.<List<String>>pipeline("update-product")
                    .step("audit", trace -> trace.add("audit"))
                    .requiredStep("save", trace -> trace.add("save"))
                    .build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateConfig {

        @Bean
        Pipeline<List<String>> anotherUpdateProductPipeline(PipelineFactory factory) {
            return factory.<List<String>>pipeline("update-product")
                    .step("other", trace -> trace.add("other"))
                    .build();
        }
    }
}
