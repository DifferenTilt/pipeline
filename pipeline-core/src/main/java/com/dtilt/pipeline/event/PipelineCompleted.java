package com.dtilt.pipeline.event;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Emitted when all steps have been executed or skipped without errors.
 *
 * @param pipeline      the pipeline name
 * @param runId         the run identifier, the same for all events of one execution
 * @param occurredAt    the instant of the event, read from the configured {@code Clock}
 * @param context       the execution context; listeners must not modify it
 * @param duration      the total duration, measured with {@code System.nanoTime()}
 * @param executedSteps the names of the executed steps, in order; unmodifiable list
 * @param skippedSteps  the names of the skipped steps, in order; unmodifiable list
 * @param <C>           the context type
 */
public record PipelineCompleted<C>(String pipeline, String runId, Instant occurredAt, C context,
                                   Duration duration, List<String> executedSteps,
                                   List<String> skippedSteps)
        implements PipelineEvent<C> {

    /** Defensively copies the lists, making them unmodifiable. */
    public PipelineCompleted {
        executedSteps = List.copyOf(executedSteps);
        skippedSteps = List.copyOf(skippedSteps);
    }
}
