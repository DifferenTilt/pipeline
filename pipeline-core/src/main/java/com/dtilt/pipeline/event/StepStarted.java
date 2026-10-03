package com.dtilt.pipeline.event;

import java.time.Instant;

/**
 * Emesso subito prima dell'esecuzione di uno step attivo.
 *
 * @param pipeline   nome della pipeline
 * @param runId      identificativo dell'esecuzione, uguale per tutti gli eventi della stessa esecuzione
 * @param occurredAt istante dell'evento, letto dal {@code Clock} configurato
 * @param context    contesto dell'esecuzione; i listener non devono modificarlo
 * @param step       nome dello step
 * @param stepIndex  posizione dello step nella pipeline, a partire da 0, contando anche gli step saltati
 * @param <C>        tipo del contesto
 */
public record StepStarted<C>(String pipeline, String runId, Instant occurredAt, C context, String step, int stepIndex)
        implements PipelineEvent<C> {
}
