package com.dtilt.pipeline.event;

import java.time.Instant;

/**
 * Emitted right before an enabled step is executed.
 *
 * @param pipeline   the pipeline name
 * @param runId      the run identifier, the same for all events of one execution
 * @param occurredAt the instant of the event, read from the configured {@code Clock}
 * @param context    the execution context; listeners must not modify it
 * @param step       the step name
 * @param stepIndex  the position of the step in the pipeline, starting from 0 and counting skipped steps
 * @param <C>        the context type
 */
public record StepStarted<C>(String pipeline, String runId, Instant occurredAt, C context, String step, int stepIndex)
        implements PipelineEvent<C> {
}
