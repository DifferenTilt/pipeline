package com.dtilt.pipeline.event;

import com.dtilt.pipeline.BusinessRuleException;
import com.dtilt.pipeline.PipelineException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.Objects;

/**
 * Listener opzionale che logga il ciclo di vita delle pipeline con {@link System.Logger}, usando
 * il logger di nome {@value #LOGGER_NAME}. Non viene registrato automaticamente.
 *
 * <ul>
 *   <li>avvio della pipeline ed eventi degli step: {@code DEBUG};</li>
 *   <li>pipeline completata: {@code INFO};</li>
 *   <li>pipeline fallita per una {@link BusinessRuleException}: {@code INFO}, con il codice;</li>
 *   <li>pipeline fallita per un errore tecnico: {@code WARNING}, con il messaggio ma senza stack
 *       trace (lo logga chi gestisce l'eccezione).</li>
 * </ul>
 *
 * <p>{@link StepFailed} non viene loggato, per evitare duplicati con {@link PipelineFailed}.
 */
public class LoggingPipelineListener implements PipelineListener<Object> {

    /** Nome del logger usato da questo listener. */
    public static final String LOGGER_NAME = "com.dtilt.pipeline";

    private final Logger logger;

    /** Crea un listener che scrive sul logger di nome {@value #LOGGER_NAME}. */
    public LoggingPipelineListener() {
        this(System.getLogger(LOGGER_NAME));
    }

    LoggingPipelineListener(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public void onPipelineStarted(PipelineStarted<?> event) {
        logger.log(Level.DEBUG, () -> "Pipeline '" + event.pipeline() + "' started" + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onStepStarted(StepStarted<?> event) {
        logger.log(Level.DEBUG, () -> "Step '" + event.step() + "' of pipeline '" + event.pipeline()
                + "' started" + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onStepCompleted(StepCompleted<?> event) {
        logger.log(Level.DEBUG, () -> "Step '" + event.step() + "' of pipeline '" + event.pipeline()
                + "' completed in " + format(event.duration()) + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onStepSkipped(StepSkipped<?> event) {
        logger.log(Level.DEBUG, () -> "Step '" + event.step() + "' of pipeline '" + event.pipeline()
                + "' skipped" + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onPipelineCompleted(PipelineCompleted<?> event) {
        logger.log(Level.INFO, () -> "Pipeline '" + event.pipeline() + "' completed in "
                + format(event.duration()) + ": " + event.executedSteps().size() + " step(s) executed, "
                + event.skippedSteps().size() + " skipped" + runId(event));
    }

    /** {@inheritDoc} */
    @Override
    public void onPipelineFailed(PipelineFailed<?> event) {
        PipelineException error = event.error();
        if (error instanceof BusinessRuleException businessError) {
            logger.log(Level.INFO, () -> "Pipeline '" + event.pipeline() + "' rejected at step '"
                    + event.failedStep() + "' with code " + businessError.getCode() + runId(event));
        } else {
            logger.log(Level.WARNING, () -> "Pipeline '" + event.pipeline() + "' failed at step '"
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
