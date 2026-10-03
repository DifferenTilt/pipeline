package com.dtilt.pipeline.event;

import com.dtilt.pipeline.exception.PipelineException;
import java.time.Duration;
import java.time.Instant;

/**
 * Emesso quando l'esecuzione si interrompe per il fallimento di uno step, dopo {@link StepFailed}.
 *
 * @param pipeline   nome della pipeline
 * @param runId      identificativo dell'esecuzione, uguale per tutti gli eventi della stessa esecuzione
 * @param occurredAt istante dell'evento, letto dal {@code Clock} configurato
 * @param context    contesto dell'esecuzione; i listener non devono modificarlo
 * @param duration   durata totale fino al fallimento, misurata con {@code System.nanoTime()}
 * @param failedStep nome dello step che ha causato il fallimento
 * @param error      eccezione già tradotta e localizzata: la stessa istanza lanciata al chiamante
 * @param <C>        tipo del contesto
 */
public record PipelineFailed<C>(String pipeline, String runId, Instant occurredAt, C context,
                                Duration duration, String failedStep, PipelineException error)
        implements PipelineEvent<C> {
}
