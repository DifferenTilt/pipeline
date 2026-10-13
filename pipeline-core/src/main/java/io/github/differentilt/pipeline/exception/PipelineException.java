package io.github.differentilt.pipeline.exception;

import io.github.differentilt.pipeline.Pipeline;

/**
 * Root of the hierarchy of exceptions propagated by {@link Pipeline#run(Object)}.
 *
 * <p>Every instance can be <em>located</em>: the pipeline that catches it records its own name
 * and the name of the step in which the error occurred. With nested pipelines the innermost
 * location, which is the most precise one, is kept.
 */
public abstract class PipelineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String pipeline;
    private String step;

    /**
     * Creates an exception that has no location yet.
     *
     * @param message the error message
     */
    protected PipelineException(String message) {
        super(message);
    }

    /**
     * Creates an exception that has no location yet, with a cause.
     *
     * @param message the error message
     * @param cause   the original exception
     */
    protected PipelineException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * The name of the pipeline in which the error occurred.
     *
     * @return the pipeline name; {@code null} if thrown outside a pipeline
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * The name of the step in which the error occurred.
     *
     * @return the step name; {@code null} if thrown outside a pipeline
     */
    public String getStep() {
        return step;
    }

    /**
     * Sets the location of the error, only if it has not been set already: with nested pipelines
     * the innermost one is therefore kept. Reserved for {@link Pipeline}, which calls it when it
     * catches the exception; application code has no reason to use it.
     *
     * @param pipeline the name of the pipeline in which the error occurred
     * @param step     the name of the step in which the error occurred
     */
    public final void locate(String pipeline, String step) {
        if (this.pipeline == null && this.step == null) {
            this.pipeline = pipeline;
            this.step = step;
        }
    }
}
