package io.github.differentilt.pipeline.event;

import io.github.differentilt.pipeline.listener.PipelineListener;
import java.time.Instant;

/**
 * A pipeline lifecycle event, delivered to {@link PipelineListener}s.
 *
 * @param <C> the context type
 */
public sealed interface PipelineEvent<C>
        permits PipelineStarted, StepStarted, StepSkipped, StepCompleted,
                StepFailed, PipelineCompleted, PipelineFailed {

    /**
     * The name of the pipeline that emitted the event.
     *
     * @return the pipeline name
     */
    String pipeline();

    /**
     * The run identifier, the same for all events of one execution.
     *
     * @return the run identifier
     */
    String runId();

    /**
     * The instant of the event, read from the {@code Clock} configured on the pipeline.
     *
     * @return the instant of the event
     */
    Instant occurredAt();

    /**
     * The execution context. Listeners must not modify it.
     *
     * @return the context
     */
    C context();
}
