package io.github.differentilt.pipeline.listener;

import io.github.differentilt.pipeline.event.PipelineCompleted;
import io.github.differentilt.pipeline.event.PipelineEvent;
import io.github.differentilt.pipeline.event.PipelineFailed;
import io.github.differentilt.pipeline.event.PipelineStarted;
import io.github.differentilt.pipeline.event.StepCompleted;
import io.github.differentilt.pipeline.event.StepFailed;
import io.github.differentilt.pipeline.event.StepSkipped;
import io.github.differentilt.pipeline.event.StepStarted;
import io.github.differentilt.pipeline.exception.BusinessRuleException;
import io.github.differentilt.pipeline.exception.PipelineException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Objects;

/**
 * An optional listener that logs the pipeline lifecycle with {@link System.Logger}, using the
 * logger named {@value #LOGGER_NAME}. It is not registered automatically.
 *
 * <ul>
 *   <li>pipeline start and step events: {@code DEBUG};</li>
 *   <li>pipeline completed: {@code INFO};</li>
 *   <li>pipeline failed with a {@link BusinessRuleException}: {@code INFO}, with the code;</li>
 *   <li>pipeline failed with a technical error: {@code WARNING}, with the message but without
 *       the stack trace (whoever handles the exception logs it).</li>
 * </ul>
 *
 * <p>{@link StepFailed} is not logged, to avoid duplicating {@link PipelineFailed}.
 */
public class LoggingPipelineListener implements PipelineListener<Object> {

    /** The name of the logger used by this listener. */
    public static final String LOGGER_NAME = "io.github.differentilt.pipeline";
    public static final String STEP = "Step '";
    public static final String OF_PIPELINE = "' of pipeline '";
    public static final String PIPELINE = "Pipeline '";

    private final Logger logger;

    /** Creates a listener that writes to the logger named {@value #LOGGER_NAME}. */
    public LoggingPipelineListener() {
        this(System.getLogger(LOGGER_NAME));
    }

    LoggingPipelineListener(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public void onPipelineStarted(PipelineStarted<?> event) {
        logger.log(Level.DEBUG, () -> PIPELINE + event.pipeline() + "' started" + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onStepStarted(StepStarted<?> event) {
        logger.log(Level.DEBUG, () -> STEP + event.step() + OF_PIPELINE + event.pipeline()
                + "' started" + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onStepCompleted(StepCompleted<?> event) {
        logger.log(Level.DEBUG, () -> STEP + event.step() + OF_PIPELINE + event.pipeline()
                + "' completed in " + format(event.duration()) + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onStepSkipped(StepSkipped<?> event) {
        logger.log(Level.DEBUG, () -> STEP + event.step() + OF_PIPELINE + event.pipeline()
                + "' skipped" + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onPipelineCompleted(PipelineCompleted<?> event) {
        logger.log(Level.INFO, () -> PIPELINE + event.pipeline() + "' completed in "
                + format(event.duration()) + ": " + event.executedSteps().size() + " step(s) executed, "
                + event.skippedSteps().size() + " skipped" + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onPipelineFailed(PipelineFailed<?> event) {
        PipelineException error = event.error();
        if (error instanceof BusinessRuleException businessError) {
            logger.log(Level.INFO, () -> PIPELINE + event.pipeline() + "' rejected at step '"
                    + event.failedStep() + "' with code " + businessError.getCode() + runId(event));
        } else {
            logger.log(Level.WARNING, () -> PIPELINE + event.pipeline() + "' failed at step '"
                    + event.failedStep() + "': " + error.getMessage() + runId(event));
        }
    }

    private static String runId(PipelineEvent<?> event) {
        return " [runId=" + event.runId() + "]";
    }

    private static String format(Duration duration) {
        return duration.toMillis() + " ms";
    }
}
