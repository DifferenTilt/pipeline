package io.github.differentilt.pipeline.spring;

/**
 * Signals an invalid pipeline configuration: malformed or unknown entries in
 * {@code pipeline.disabled-steps}, duplicate pipeline names, HTTP statuses that are not allowed.
 * It is thrown at startup and prevents the application from starting.
 */
public class PipelineConfigurationException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message a description of the problems found
     */
    public PipelineConfigurationException(String message) {
        super(message);
    }
}
