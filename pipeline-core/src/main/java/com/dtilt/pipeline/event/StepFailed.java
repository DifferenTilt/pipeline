package com.dtilt.pipeline.event;

import com.dtilt.pipeline.exception.PipelineException;
import java.time.Duration;
import java.time.Instant;

/**
 * Emesso quando uno step fallisce, subito prima di {@link PipelineFailed}.
 *
 * <p>Di norma è preceduto da {@link StepStarted} per lo stesso step. Unica eccezione: se è la
 * valutazione dell'attivazione dello step a lanciare un'eccezione, {@code StepFailed} viene
 * emesso senza un {@code StepStarted} precedente e con durata {@link Duration#ZERO}.
 *
 * @param pipeline   nome della pipeline
 * @param runId      identificativo dell'esecuzione, uguale per tutti gli eventi della stessa esecuzione
 * @param occurredAt istante dell'evento, letto dal {@code Clock} configurato
 * @param context    contesto dell'esecuzione; i listener non devono modificarlo
 * @param step       nome dello step
 * @param stepIndex  posizione dello step nella pipeline, a partire da 0, contando anche gli step saltati
 * @param duration   durata dello step, misurata con {@code System.nanoTime()}
 * @param error      eccezione già tradotta e localizzata: la stessa istanza lanciata al chiamante
 * @param <C>        tipo del contesto
 */
public record StepFailed<C>(String pipeline, String runId, Instant occurredAt, C context,
                            String step, int stepIndex, Duration duration, PipelineException error)
        implements PipelineEvent<C> {
}
