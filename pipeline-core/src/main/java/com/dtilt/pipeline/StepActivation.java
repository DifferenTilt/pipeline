package com.dtilt.pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Decide, per ogni esecuzione, se uno step è attivo. Gli step disattivati vengono saltati.
 *
 * <p>{@link #isEnabled(String, String)} viene chiamato a ogni esecuzione per ogni step, non al
 * momento della costruzione della pipeline: un'implementazione può quindi cambiare risposta a
 * runtime. Le implementazioni devono essere thread-safe se la pipeline viene eseguita da più
 * thread.
 */
@FunctionalInterface
public interface StepActivation {

    /**
     * Indica se lo step è attivo per l'esecuzione corrente.
     *
     * @param pipeline nome della pipeline
     * @param step     nome dello step
     * @return {@code true} se lo step va eseguito, {@code false} se va saltato
     */
    boolean isEnabled(String pipeline, String step);

    /** Tutti gli step sempre attivi. Default quando non viene configurata un'attivazione. */
    StepActivation ALWAYS = (pipeline, step) -> true;

    /**
     * Attivazione basata su una mappa nome-pipeline -&gt; nomi degli step disattivati.
     * Pipeline o step assenti dalla mappa sono attivi. La mappa viene copiata in modo difensivo.
     *
     * @param disabledStepsByPipeline step disattivati per nome di pipeline; non null, senza chiavi
     *                                o valori null
     * @return un'attivazione immutabile
     */
    static StepActivation disabling(Map<String, Set<String>> disabledStepsByPipeline) {
        Objects.requireNonNull(disabledStepsByPipeline, "disabledStepsByPipeline must not be null");
        Map<String, Set<String>> copy = new HashMap<>();
        disabledStepsByPipeline.forEach((pipeline, steps) -> {
            Objects.requireNonNull(pipeline, "pipeline name must not be null");
            Objects.requireNonNull(steps, "disabled steps of pipeline '" + pipeline + "' must not be null");
            copy.put(pipeline, Set.copyOf(steps));
        });
        return (pipeline, step) -> {
            Set<String> disabled = copy.get(pipeline);
            return disabled == null || !disabled.contains(step);
        };
    }
}
