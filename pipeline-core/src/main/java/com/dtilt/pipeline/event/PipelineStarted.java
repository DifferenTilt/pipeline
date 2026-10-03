package com.dtilt.pipeline.event;

import java.time.Instant;

/**
 * Emesso all'avvio di un'esecuzione, prima di qualsiasi step.
 *
 * @param pipeline   nome della pipeline
 * @param runId      identificativo dell'esecuzione, uguale per tutti gli eventi della stessa esecuzione
 * @param occurredAt istante dell'evento, letto dal {@code Clock} configurato
 * @param context    contesto dell'esecuzione; i listener non devono modificarlo
 * @param <C>        tipo del contesto
 */
public record PipelineStarted<C>(String pipeline, String runId, Instant occurredAt, C context)
        implements PipelineEvent<C> {
}
