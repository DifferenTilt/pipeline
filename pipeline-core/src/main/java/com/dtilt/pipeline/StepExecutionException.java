package com.dtilt.pipeline;

import java.util.Objects;

/**
 * Errore tecnico inatteso avvenuto durante l'esecuzione di uno step. Viene creata dalla pipeline
 * quando uno step (o la valutazione della sua attivazione) lancia una {@link RuntimeException}
 * che non è una {@link PipelineException}.
 *
 * <p>È sempre localizzata e {@link #getCause()} restituisce l'eccezione originale.
 */
public class StepExecutionException extends PipelineException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea l'eccezione per lo step indicato.
     *
     * @param pipeline nome della pipeline in cui è avvenuto l'errore; non null
     * @param step     nome dello step in cui è avvenuto l'errore; non null
     * @param cause    eccezione originale; non null
     */
    // locate è final e si limita ad assegnare due campi: nessuna fuga reale di this.
    @SuppressWarnings("this-escape")
    public StepExecutionException(String pipeline, String step, Throwable cause) {
        super(message(pipeline, step, cause), cause);
        locate(pipeline, step);
    }

    private static String message(String pipeline, String step, Throwable cause) {
        Objects.requireNonNull(pipeline, "pipeline must not be null");
        Objects.requireNonNull(step, "step must not be null");
        Objects.requireNonNull(cause, "cause must not be null");
        return "Step '" + step + "' of pipeline '" + pipeline + "' failed: " + cause.getMessage();
    }
}
