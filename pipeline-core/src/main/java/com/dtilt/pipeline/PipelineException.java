package com.dtilt.pipeline;

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

    /** Assegna la posizione solo se non è già stata assegnata. */
    final void locate(String pipeline, String step) {
        if (this.pipeline == null && this.step == null) {
            this.pipeline = pipeline;
            this.step = step;
        }
    }
}
