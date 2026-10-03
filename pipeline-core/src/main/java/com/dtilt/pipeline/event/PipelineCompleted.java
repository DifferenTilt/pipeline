package com.dtilt.pipeline.event;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Emesso quando tutti gli step sono stati eseguiti o saltati senza errori.
 *
 * @param pipeline   nome della pipeline
 * @param runId      identificativo dell'esecuzione, uguale per tutti gli eventi della stessa esecuzione
 * @param occurredAt istante dell'evento, letto dal {@code Clock} configurato
 * @param context    contesto dell'esecuzione; i listener non devono modificarlo
 * @param duration      durata totale, misurata con {@code System.nanoTime()}
 * @param executedSteps nomi degli step eseguiti, in ordine; lista non modificabile
 * @param skippedSteps  nomi degli step saltati, in ordine; lista non modificabile
 * @param <C>        tipo del contesto
 */
public record PipelineCompleted<C>(String pipeline, String runId, Instant occurredAt, C context,
                                   Duration duration, List<String> executedSteps,
                                   List<String> skippedSteps)
        implements PipelineEvent<C> {

    /** Copia le liste in modo difensivo, rendendole non modificabili. */
    public PipelineCompleted {
        executedSteps = List.copyOf(executedSteps);
        skippedSteps = List.copyOf(skippedSteps);
    }
}
