package com.dtilt.pipeline.spring;

/**
 * Segnala una configurazione delle pipeline non valida: voci di {@code pipeline.disabled-steps}
 * malformate o sconosciute, nomi di pipeline duplicati, status HTTP non ammessi. Lanciata
 * all'avvio, impedisce la partenza dell'applicazione.
 */
public class PipelineConfigurationException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea l'eccezione.
     *
     * @param message descrizione dei problemi riscontrati
     */
    public PipelineConfigurationException(String message) {
        super(message);
    }
}
