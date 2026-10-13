package io.github.differentilt.pipeline.event;

import io.github.differentilt.pipeline.exception.PipelineException;
import java.time.Duration;
import java.time.Instant;

/**
 * Emitted when a step fails, right before {@link PipelineFailed}.
 *
 * <p>It is normally preceded by {@link StepStarted} for the same step. The one exception: if it
 * is the evaluation of the step's activation that throws, {@code StepFailed} is emitted without a
 * preceding {@code StepStarted} and with a duration of {@link Duration#ZERO}.
 *
 * @param pipeline   the pipeline name
 * @param runId      the run identifier, the same for all events of one execution
 * @param occurredAt the instant of the event, read from the configured {@code Clock}
 * @param context    the execution context; listeners must not modify it
 * @param step       the step name
 * @param stepIndex  the position of the step in the pipeline, starting from 0 and counting skipped steps
 * @param duration   the duration of the step, measured with {@code System.nanoTime()}
 * @param error      the exception, already translated and located: the same instance thrown to the caller
 * @param <C>        the context type
 */
public record StepFailed<C>(String pipeline, String runId, Instant occurredAt, C context,
                            String step, int stepIndex, Duration duration, PipelineException error)
        implements PipelineEvent<C> {
}
