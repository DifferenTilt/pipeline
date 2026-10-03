package com.dtilt.pipeline.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dtilt.pipeline.Pipeline;
import com.dtilt.pipeline.PipelineFactory;
import com.dtilt.pipeline.StepActivation;
import com.dtilt.pipeline.event.LoggingPipelineListener;
import com.dtilt.pipeline.event.PipelineEvent;
import com.dtilt.pipeline.event.PipelineListener;
import com.dtilt.pipeline.spring.web.PipelineWebAutoConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.SystemEnvironmentPropertySource;

@ExtendWith(OutputCaptureExtension.class)
class PipelineAutoConfigurationTests {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-02T03:04:05Z");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PipelineAutoConfiguration.class, PipelineWebAutoConfiguration.class));

    /** Pipeline a, b, c il cui contesto registra i nomi degli step eseguiti. */
    private static Pipeline<List<String>> abc(PipelineFactory factory, String name) {
        return factory.<List<String>>pipeline(name)
                .step("a", trace -> trace.add("a"))
                .step("b", trace -> trace.add("b"))
                .step("c", trace -> trace.add("c"))
                .build();
    }

    private static List<String> run(PipelineFactory factory, String name) {
        return abc(factory, name).run(new ArrayList<>());
    }

    // --- Auto-configuration di base ---

    @Test
    void withoutPropertiesProvidesDefaultBeansAndAllStepsAreEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(StepActivation.class);
            assertThat(context).hasSingleBean(PipelineFactory.class);
            assertThat(context).hasSingleBean(PipelineDefinitionsValidator.class);
            assertThat(context).doesNotHaveBean(LoggingPipelineListener.class);
            assertThat(context.getBean(StepActivation.class).isEnabled("any", "step")).isTrue();
            assertThat(run(context.getBean(PipelineFactory.class), "p")).containsExactly("a", "b", "c");
        });
    }

    @Test
    void projectStepActivationReplacesTheDefault() {
        contextRunner.withUserConfiguration(CustomActivationConfig.class).run(context -> {
            assertThat(context).hasSingleBean(StepActivation.class);
            assertThat(context.getBean(StepActivation.class)).isSameAs(CustomActivationConfig.ACTIVATION);
            assertThat(run(context.getBean(PipelineFactory.class), "p")).containsExactly("a", "b");
        });
    }

    @Test
    void projectPipelineFactoryReplacesTheDefault() {
        contextRunner.withUserConfiguration(CustomFactoryConfig.class).run(context -> {
            assertThat(context).hasSingleBean(PipelineFactory.class);
            assertThat(context.getBean(PipelineFactory.class)).isSameAs(CustomFactoryConfig.FACTORY);
        });
    }

    // --- pipeline.disabled-steps ---

    @Test
    void validEntryDisablesOnlyThatStepOfThatPipeline() {
        contextRunner.withPropertyValues("pipeline.disabled-steps[0]=first.b",
                        "pipeline.fail-on-unknown-steps=false")
                .run(context -> {
                    PipelineFactory factory = context.getBean(PipelineFactory.class);
                    assertThat(run(factory, "first")).containsExactly("a", "c");
                    assertThat(run(factory, "second")).containsExactly("a", "b", "c");
                });
    }

    @Test
    void commaSeparatedValueIsReadAsMultipleEntries() {
        contextRunner.withPropertyValues("pipeline.disabled-steps=first.b,second.c",
                        "pipeline.fail-on-unknown-steps=false")
                .run(context -> {
                    assertThat(context.getBean(PipelineProperties.class).getDisabledSteps())
                            .containsExactly("first.b", "second.c");
                    PipelineFactory factory = context.getBean(PipelineFactory.class);
                    assertThat(run(factory, "first")).containsExactly("a", "c");
                    assertThat(run(factory, "second")).containsExactly("a", "b");
                });
    }

    @Test
    void environmentVariableFormIsSupported() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new SystemEnvironmentPropertySource("test-systemEnvironment",
                                Map.of("PIPELINE_DISABLEDSTEPS", "first.b,second.c",
                                        "PIPELINE_FAILONUNKNOWNSTEPS", "false"))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(PipelineProperties.class).getDisabledSteps())
                            .containsExactly("first.b", "second.c");
                    assertThat(run(context.getBean(PipelineFactory.class), "first")).containsExactly("a", "c");
                });
    }

    @Test
    void whitespaceAroundEntriesIsRemoved() {
        contextRunner.withPropertyValues("pipeline.disabled-steps[0]=  first.b  ",
                        "pipeline.disabled-steps[1]=\tfirst.c ",
                        "pipeline.fail-on-unknown-steps=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(run(context.getBean(PipelineFactory.class), "first")).containsExactly("a");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"nodot", "a.b.c", "Create.step", ""})
    void malformedEntryPreventsStartup(String entry) {
        contextRunner.withPropertyValues("pipeline.disabled-steps[0]=" + entry,
                        "pipeline.fail-on-unknown-steps=false")
                .run(context -> assertThat(StartupFailures.rootCause(context))
                        .isInstanceOf(PipelineConfigurationException.class)
                        .hasMessageContaining("'" + entry + "'"));
    }

    @Test
    void malformedEntryPreventsStartupEvenWithProjectStepActivation() {
        contextRunner.withUserConfiguration(CustomActivationConfig.class)
                .withPropertyValues("pipeline.disabled-steps=Not-Valid")
                .run(context -> assertThat(StartupFailures.rootCause(context))
                        .isInstanceOf(PipelineConfigurationException.class)
                        .hasMessageContaining("'Not-Valid'"));
    }

    // --- Listener ---

    @Test
    void objectListenersAreGlobalAndNotifiedInOrder() {
        contextRunner.withUserConfiguration(OrderedListenersConfig.class).run(context -> {
            OrderedListenersConfig config = context.getBean(OrderedListenersConfig.class);

            run(context.getBean(PipelineFactory.class), "p");

            assertThat(config.calls).isNotEmpty();
            assertThat(config.calls.subList(0, 3))
                    .containsExactly("first:PipelineStarted", "second:PipelineStarted", "third:PipelineStarted");
        });
    }

    @Test
    void contextSpecificListenerIsNotRegisteredAsGlobal() {
        contextRunner.withUserConfiguration(SpecificListenerConfig.class).run(context -> {
            SpecificListenerConfig config = context.getBean(SpecificListenerConfig.class);
            PipelineFactory factory = context.getBean(PipelineFactory.class);

            run(factory, "p");

            assertThat(config.specific.events).isEmpty();
            assertThat(config.global.events).hasSize(8);

            // Va aggiunto esplicitamente sul builder della pipeline a cui si riferisce.
            factory.<List<String>>pipeline("explicit")
                    .step("a", trace -> trace.add("a"))
                    .listener(config.specific)
                    .build()
                    .run(new ArrayList<>());

            assertThat(config.specific.events).hasSize(4);
        });
    }

    @Test
    void loggingListenerIsRegisteredOnlyWhenEnabled(CapturedOutput output) {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(LoggingPipelineListener.class));
        contextRunner.withPropertyValues("pipeline.logging.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LoggingPipelineListener.class));

        contextRunner.withPropertyValues("pipeline.logging.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(LoggingPipelineListener.class);

            run(context.getBean(PipelineFactory.class), "logged-pipeline");

            assertThat(output).contains("Pipeline 'logged-pipeline' completed");
        });
    }

    @Test
    void projectLoggingListenerReplacesTheDefault() {
        contextRunner.withUserConfiguration(CustomLoggingListenerConfig.class)
                .withPropertyValues("pipeline.logging.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(LoggingPipelineListener.class);
                    assertThat(context.getBean(LoggingPipelineListener.class))
                            .isSameAs(CustomLoggingListenerConfig.LISTENER);
                });
    }

    @Test
    void listenerErrorPolicyDefaultsToLogAndContinue() {
        contextRunner.withUserConfiguration(BrokenListenerConfig.class).run(context ->
                assertThat(run(context.getBean(PipelineFactory.class), "p")).containsExactly("a", "b", "c"));
    }

    @Test
    void propagatePolicyLetsListenerExceptionsReachTheCaller() {
        contextRunner.withUserConfiguration(BrokenListenerConfig.class)
                .withPropertyValues("pipeline.listener-error-policy=PROPAGATE")
                .run(context -> {
                    PipelineFactory factory = context.getBean(PipelineFactory.class);

                    assertThatThrownBy(() -> run(factory, "p")).isSameAs(BrokenListenerConfig.ERROR);
                });
    }

    @Test
    void clockBeanIsUsedForEventTimestamps() {
        contextRunner.withUserConfiguration(FixedClockConfig.class).run(context -> {
            FixedClockConfig config = context.getBean(FixedClockConfig.class);

            run(context.getBean(PipelineFactory.class), "p");

            assertThat(config.listener.events).hasSize(8)
                    .allSatisfy(event -> assertThat(event.occurredAt()).isEqualTo(FIXED_INSTANT));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomActivationConfig {

        static final StepActivation ACTIVATION = (pipeline, step) -> !step.equals("c");

        @Bean
        StepActivation customActivation() {
            return ACTIVATION;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomFactoryConfig {

        static final PipelineFactory FACTORY = PipelineFactory.builder().build();

        @Bean
        PipelineFactory customFactory() {
            return FACTORY;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedListenersConfig {

        final List<String> calls = new ArrayList<>();

        private PipelineListener<Object> named(String name) {
            return new PipelineListener<>() {
                @Override
                public void onEvent(PipelineEvent<?> event) {
                    calls.add(name + ":" + event.getClass().getSimpleName());
                }
            };
        }

        @Bean
        @Order(3)
        PipelineListener<Object> thirdListener() {
            return named("third");
        }

        @Bean
        @Order(1)
        PipelineListener<Object> firstListener() {
            return named("first");
        }

        @Bean
        @Order(2)
        PipelineListener<Object> secondListener() {
            return named("second");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SpecificListenerConfig {

        final RecordingListener<List<String>> specific = new RecordingListener<>();
        final RecordingListener<Object> global = new RecordingListener<>();

        @Bean
        PipelineListener<List<String>> specificListener() {
            return specific;
        }

        @Bean
        PipelineListener<Object> globalListener() {
            return global;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomLoggingListenerConfig {

        static final LoggingPipelineListener LISTENER = new LoggingPipelineListener();

        @Bean
        LoggingPipelineListener customLoggingListener() {
            return LISTENER;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class BrokenListenerConfig {

        static final IllegalStateException ERROR = new IllegalStateException("listener is broken");

        @Bean
        PipelineListener<Object> brokenListener() {
            return new PipelineListener<>() {
                @Override
                public void onEvent(PipelineEvent<?> event) {
                    throw ERROR;
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class FixedClockConfig {

        final RecordingListener<Object> listener = new RecordingListener<>();

        @Bean
        Clock clock() {
            return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        }

        @Bean
        PipelineListener<Object> recordingListener() {
            return listener;
        }
    }
}
