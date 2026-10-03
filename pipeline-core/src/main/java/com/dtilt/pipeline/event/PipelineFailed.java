package com.dtilt.pipeline.event;

import com.dtilt.pipeline.exception.PipelineException;
import java.time.Duration;
import java.time.Instant;

/**
 * Emitted when the execution stops because a step failed, after {@link StepFailed}.
 *
 * @param pipeline   the pipeline name
 * @param runId      the run identifier, the same for all events of one execution
 * @param occurredAt the instant of the event, read from the configured {@code Clock}
 * @param context    the execution context; listeners must not modify it
 * @param duration   the total duration up to the failure, measured with {@code System.nanoTime()}
 * @param failedStep the name of the step that caused the failure
 * @param error      the exception, already translated and located: the same instance thrown to the caller
 * @param <C>        the context type
 */
public record PipelineFailed<C>(String pipeline, String runId, Instant occurredAt, C context,
                                Duration duration, String failedStep, PipelineException error)
        implements PipelineEvent<C> {
}
