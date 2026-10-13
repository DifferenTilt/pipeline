package io.github.differentilt.pipeline.event;

import java.time.Instant;

/**
 * Emitted when an execution starts, before any step.
 *
 * @param pipeline   the pipeline name
 * @param runId      the run identifier, the same for all events of one execution
 * @param occurredAt the instant of the event, read from the configured {@code Clock}
 * @param context    the execution context; listeners must not modify it
 * @param <C>        the context type
 */
public record PipelineStarted<C>(String pipeline, String runId, Instant occurredAt, C context)
        implements PipelineEvent<C> {
}
