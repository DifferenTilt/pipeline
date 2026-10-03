package com.dtilt.pipeline.spring;

import com.dtilt.pipeline.PipelineFactory;
import com.dtilt.pipeline.step.StepActivation;
import com.dtilt.pipeline.listener.LoggingPipelineListener;
import com.dtilt.pipeline.listener.PipelineListener;
import java.time.Clock;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Base auto-configuration: exposes a {@link StepActivation} built from
 * {@code pipeline.disabled-steps}, a {@link PipelineFactory} already wired to the activation,
 * the global listeners, the error policy and the {@link Clock}, and the
 * {@link PipelineDefinitionsValidator} that checks the configuration at startup.
 *
 * <p>{@code StepActivation}, {@code PipelineFactory} and {@code LoggingPipelineListener} are
 * only created if the project does not define its own bean of the same type.
 */
@AutoConfiguration
@EnableConfigurationProperties(PipelineProperties.class)
public class PipelineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    StepActivation pipelineStepActivation(PipelineProperties properties) {
        return StepActivation.disabling(DisabledStepsParser.parse(properties.getDisabledSteps()));
    }

    @Bean
    @ConditionalOnProperty(name = "pipeline.logging.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    LoggingPipelineListener loggingPipelineListener() {
        return new LoggingPipelineListener();
    }

    @Bean
    @ConditionalOnMissingBean
    PipelineFactory pipelineFactory(StepActivation activation,
                                    ObjectProvider<PipelineListener<Object>> listeners,
                                    ObjectProvider<Clock> clock,
                                    PipelineProperties properties) {
        PipelineFactory.Builder builder = PipelineFactory.builder()
                .activation(activation)
                .listenerErrorPolicy(properties.getListenerErrorPolicy());
        listeners.orderedStream().forEach(builder::listener);
        Clock availableClock = clock.getIfAvailable();
        if (availableClock != null) {
            builder.clock(availableClock);
        }
        return builder.build();
    }

    @Bean
    PipelineDefinitionsValidator pipelineDefinitionsValidator(ListableBeanFactory beanFactory,
                                                              PipelineProperties properties) {
        return new PipelineDefinitionsValidator(beanFactory, properties);
    }
}
