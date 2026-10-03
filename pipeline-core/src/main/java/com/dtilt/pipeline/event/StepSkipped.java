package com.dtilt.pipeline.event;

import java.time.Instant;

/**
 * Emesso quando uno step viene saltato perché disattivato.
 *
 * @param pipeline   nome della pipeline
 * @param runId      identificativo dell'esecuzione, uguale per tutti gli eventi della stessa esecuzione
 * @param occurredAt istante dell'evento, letto dal {@code Clock} configurato
 * @param context    contesto dell'esecuzione; i listener non devono modificarlo
 * @param step       nome dello step
 * @param stepIndex  posizione dello step nella pipeline, a partire da 0, contando anche gli step saltati
 * @param <C>        tipo del contesto
 */
public record StepSkipped<C>(String pipeline, String runId, Instant occurredAt, C context, String step, int stepIndex)
        implements PipelineEvent<C> {
}
