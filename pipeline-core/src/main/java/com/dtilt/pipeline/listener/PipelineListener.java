package com.dtilt.pipeline.listener;

import com.dtilt.pipeline.event.PipelineCompleted;
import com.dtilt.pipeline.event.PipelineEvent;
import com.dtilt.pipeline.event.PipelineFailed;
import com.dtilt.pipeline.event.PipelineStarted;
import com.dtilt.pipeline.event.StepCompleted;
import com.dtilt.pipeline.event.StepFailed;
import com.dtilt.pipeline.event.StepSkipped;
import com.dtilt.pipeline.event.StepStarted;

/**
 * Riceve gli eventi del ciclo di vita di una pipeline, per logging, metriche, audit.
 *
 * <p>La pipeline chiama solo {@link #onEvent(PipelineEvent)}. Un listener può sovrascrivere
 * {@code onEvent} per gestire tutti gli eventi in un punto, oppure i singoli metodi specifici.
 *
 * <p>I listener ricevono il riferimento al contesto dell'esecuzione e <strong>non devono
 * modificarlo</strong>. Un listener registrato su una pipeline eseguita da più thread deve essere
 * thread-safe.
 *
 * <p>I parametri usano {@code ? extends C} di proposito: così un {@code PipelineListener<Object>}
 * può essere registrato su una pipeline con qualsiasi tipo di contesto.
 *
 * @param <C> tipo del contesto osservato
 */
public interface PipelineListener<C> {

    /**
     * Punto di ingresso unico. Il default smista l'evento verso il metodo specifico per il suo tipo.
     *
     * @param event evento emesso dalla pipeline
     */
    default void onEvent(PipelineEvent<? extends C> event) {
        if (event instanceof PipelineStarted<? extends C> e) {
            onPipelineStarted(e);
        } else if (event instanceof StepStarted<? extends C> e) {
            onStepStarted(e);
        } else if (event instanceof StepSkipped<? extends C> e) {
            onStepSkipped(e);
        } else if (event instanceof StepCompleted<? extends C> e) {
            onStepCompleted(e);
        } else if (event instanceof StepFailed<? extends C> e) {
            onStepFailed(e);
        } else if (event instanceof PipelineCompleted<? extends C> e) {
            onPipelineCompleted(e);
        } else if (event instanceof PipelineFailed<? extends C> e) {
            onPipelineFailed(e);
        }
    }

    /**
     * Chiamato all'avvio di un'esecuzione.
     *
     * @param event evento di avvio
     */
    default void onPipelineStarted(PipelineStarted<? extends C> event) {
    }

    /**
     * Chiamato subito prima dell'esecuzione di uno step attivo.
     *
     * @param event evento di avvio dello step
     */
    default void onStepStarted(StepStarted<? extends C> event) {
    }

    /**
     * Chiamato quando uno step viene saltato perché disattivato.
     *
     * @param event evento di step saltato
     */
    default void onStepSkipped(StepSkipped<? extends C> event) {
    }

    /**
     * Chiamato quando uno step termina normalmente.
     *
     * @param event evento di completamento dello step
     */
    default void onStepCompleted(StepCompleted<? extends C> event) {
    }

    /**
     * Chiamato quando uno step fallisce, prima di {@link #onPipelineFailed(PipelineFailed)}.
     *
     * @param event evento di fallimento dello step
     */
    default void onStepFailed(StepFailed<? extends C> event) {
    }

    /**
     * Chiamato quando l'esecuzione termina senza errori.
     *
     * @param event evento di completamento della pipeline
     */
    default void onPipelineCompleted(PipelineCompleted<? extends C> event) {
    }

    /**
     * Chiamato quando l'esecuzione si interrompe per il fallimento di uno step.
     *
     * @param event evento di fallimento della pipeline
     */
    default void onPipelineFailed(PipelineFailed<? extends C> event) {
    }
}
