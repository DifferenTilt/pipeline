package io.github.differentilt.pipeline.exception;

import java.util.Objects;

/**
 * An unexpected technical error that occurred while executing a step. It is created by the
 * pipeline when a step (or the evaluation of its activation) throws a {@link RuntimeException}
 * that is not a {@link PipelineException}.
 *
 * <p>It always has a location, and {@link #getCause()} returns the original exception.
 */
public class StepExecutionException extends PipelineException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception for the given step.
     *
     * @param pipeline the name of the pipeline in which the error occurred; not null
     * @param step     the name of the step in which the error occurred; not null
     * @param cause    the original exception; not null
     */
    // locate is final and only assigns two fields: there is no real escape of this.
    @SuppressWarnings("this-escape")
    public StepExecutionException(String pipeline, String step, Throwable cause) {
        super(message(pipeline, step, cause), cause);
        locate(pipeline, step);
    }

    private static String message(String pipeline, String step, Throwable cause) {
        Objects.requireNonNull(pipeline, "pipeline must not be null");
        Objects.requireNonNull(step, "step must not be null");
        Objects.requireNonNull(cause, "cause must not be null");
        return "Step '" + step + "' of pipeline '" + pipeline + "' failed: " + cause.getMessage();
    }
}
