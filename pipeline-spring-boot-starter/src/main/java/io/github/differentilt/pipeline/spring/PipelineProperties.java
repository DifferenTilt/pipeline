package io.github.differentilt.pipeline.spring;

import io.github.differentilt.pipeline.listener.ListenerErrorPolicy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Pipeline configuration, read from the properties with the {@code pipeline} prefix. */
@ConfigurationProperties("pipeline")
public class PipelineProperties {

    /**
     * Disabled steps, each in the form pipeline-name.step-name (for example
     * create-product.date-range).
     */
    private List<String> disabledSteps = new ArrayList<>();

    /**
     * If true, entries of disabled-steps that do not match an existing pipeline or step prevent
     * startup; if false they are only logged at WARN.
     */
    private boolean failOnUnknownSteps = true;

    /**
     * What happens when a listener throws an exception.
     */
    private ListenerErrorPolicy listenerErrorPolicy = ListenerErrorPolicy.LOG_AND_CONTINUE;

    private final Logging logging = new Logging();

    /** Creates the properties with their default values. */
    public PipelineProperties() {
    }

    /**
     * The disabled steps, in the form {@code <pipeline>.<step>}.
     *
     * @return the configured entries, never null
     */
    public List<String> getDisabledSteps() {
        return disabledSteps;
    }

    /**
     * Sets the disabled steps.
     *
     * @param disabledSteps entries in the form {@code <pipeline>.<step>}; null means an empty list
     */
    public void setDisabledSteps(List<String> disabledSteps) {
        this.disabledSteps = (disabledSteps != null) ? disabledSteps : new ArrayList<>();
    }

    /**
     * Tells whether unknown entries of {@code disabled-steps} prevent startup.
     *
     * @return {@code true} if they prevent startup (the default)
     */
    public boolean isFailOnUnknownSteps() {
        return failOnUnknownSteps;
    }

    /**
     * Sets whether unknown entries of {@code disabled-steps} prevent startup.
     *
     * @param failOnUnknownSteps {@code true} to prevent startup, {@code false} to log a WARN
     */
    public void setFailOnUnknownSteps(boolean failOnUnknownSteps) {
        this.failOnUnknownSteps = failOnUnknownSteps;
    }

    /**
     * The listener error policy passed to the {@code PipelineFactory}.
     *
     * @return the policy, never null
     */
    public ListenerErrorPolicy getListenerErrorPolicy() {
        return listenerErrorPolicy;
    }

    /**
     * Sets the listener error policy.
     *
     * @param listenerErrorPolicy the policy to use; null means the default {@code LOG_AND_CONTINUE}
     */
    public void setListenerErrorPolicy(ListenerErrorPolicy listenerErrorPolicy) {
        this.listenerErrorPolicy =
                (listenerErrorPolicy != null) ? listenerErrorPolicy : ListenerErrorPolicy.LOG_AND_CONTINUE;
    }

    /**
     * The pipeline logging configuration.
     *
     * @return the logging configuration, never null
     */
    public Logging getLogging() {
        return logging;
    }

    /** Properties with the {@code pipeline.logging} prefix. */
    public static class Logging {

        /**
         * Registers LoggingPipelineListener as a global listener.
         */
        private boolean enabled = false;

        /** Creates the properties with their default values. */
        public Logging() {
        }

        /**
         * Tells whether {@code LoggingPipelineListener} is registered as a global listener.
         *
         * @return {@code true} if the logging listener is enabled; the default is {@code false}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Enables or disables the logging listener.
         *
         * @param enabled {@code true} to register {@code LoggingPipelineListener}
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
