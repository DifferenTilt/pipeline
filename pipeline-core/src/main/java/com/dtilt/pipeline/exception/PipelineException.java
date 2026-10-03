package com.dtilt.pipeline.exception;

import com.dtilt.pipeline.Pipeline;

/**
 * Radice della gerarchia di eccezioni propagate da {@link Pipeline#run(Object)}.
 *
 * <p>Ogni istanza può essere <em>localizzata</em>: la pipeline che la intercetta vi registra il
 * proprio nome e quello dello step in cui è avvenuto l'errore. Con pipeline annidate viene
 * mantenuta la posizione più interna, cioè la più precisa.
 */
public abstract class PipelineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String pipeline;
    private String step;

    /**
     * Crea un'eccezione non ancora localizzata.
     *
     * @param message messaggio dell'errore
     */
    protected PipelineException(String message) {
        super(message);
    }

    /**
     * Crea un'eccezione non ancora localizzata, con causa.
     *
     * @param message messaggio dell'errore
     * @param cause   eccezione originale
     */
    protected PipelineException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Nome della pipeline in cui è avvenuto l'errore.
     *
     * @return il nome della pipeline; {@code null} se lanciata fuori da una pipeline
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Nome dello step in cui è avvenuto l'errore.
     *
     * @return il nome dello step; {@code null} se lanciata fuori da una pipeline
     */
    public String getStep() {
        return step;
    }

    /**
     * Assegna la posizione dell'errore, solo se non è già stata assegnata: con pipeline annidate
     * resta quindi quella più interna. Riservato a {@link Pipeline}, che lo chiama quando
     * intercetta l'eccezione; il codice applicativo non ha motivo di usarlo.
     *
     * @param pipeline nome della pipeline in cui è avvenuto l'errore
     * @param step     nome dello step in cui è avvenuto l'errore
     */
    public final void locate(String pipeline, String step) {
        if (this.pipeline == null && this.step == null) {
            this.pipeline = pipeline;
            this.step = step;
        }
    }
}
