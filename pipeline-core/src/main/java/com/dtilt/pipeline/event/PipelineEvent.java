package com.dtilt.pipeline.event;

import java.time.Instant;

/**
 * Evento del ciclo di vita di una pipeline, notificato ai {@link PipelineListener}.
 *
 * @param <C> tipo del contesto
 */
public sealed interface PipelineEvent<C>
        permits PipelineStarted, StepStarted, StepSkipped, StepCompleted,
                StepFailed, PipelineCompleted, PipelineFailed {

    /**
     * Nome della pipeline che ha emesso l'evento.
     *
     * @return il nome della pipeline
     */
    String pipeline();

    /**
     * Identificativo dell'esecuzione, uguale per tutti gli eventi della stessa esecuzione.
     *
     * @return l'identificativo dell'esecuzione
     */
    String runId();

    /**
     * Istante dell'evento, letto dal {@code Clock} configurato sulla pipeline.
     *
     * @return l'istante dell'evento
     */
    Instant occurredAt();

    /**
     * Contesto dell'esecuzione. I listener non devono modificarlo.
     *
     * @return il contesto
     */
    C context();
}
