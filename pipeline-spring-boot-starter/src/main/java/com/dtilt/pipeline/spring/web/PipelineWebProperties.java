package com.dtilt.pipeline.spring.web;

import com.dtilt.pipeline.BusinessRuleException;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurazione della gestione HTTP degli errori, letta dalle property {@code pipeline.web}. */
@ConfigurationProperties("pipeline.web")
public class PipelineWebProperties {

    /**
     * Aggiunge pipeline e step alle risposte di errore di business.
     */
    private boolean includeLocation = false;

    /**
     * Sovrascrive, voce per voce, la mappatura di default da categoria a status HTTP (400-599).
     */
    private Map<BusinessRuleException.Category, Integer> statusMapping =
            new EnumMap<>(BusinessRuleException.Category.class);

    private final ExceptionHandler exceptionHandler = new ExceptionHandler();

    /**
     * Indica se le risposte di errore di business includono {@code pipeline} e {@code step}.
     *
     * @return {@code true} se la posizione viene inclusa; default {@code false}
     */
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    /**
     * Imposta se includere {@code pipeline} e {@code step} nelle risposte di errore di business.
     *
     * @param includeLocation {@code true} per includere la posizione
     */
    public void setIncludeLocation(boolean includeLocation) {
        this.includeLocation = includeLocation;
    }

    /**
     * Mappatura personalizzata da categoria a status HTTP.
     *
     * @return le voci configurate, mai null (vuota di default)
     */
    public Map<BusinessRuleException.Category, Integer> getStatusMapping() {
        return statusMapping;
    }

    /**
     * Imposta la mappatura personalizzata da categoria a status HTTP.
     *
     * @param statusMapping status per categoria; null equivale a mappa vuota
     */
    public void setStatusMapping(Map<BusinessRuleException.Category, Integer> statusMapping) {
        this.statusMapping = (statusMapping != null)
                ? statusMapping
                : new EnumMap<>(BusinessRuleException.Category.class);
    }

    /**
     * Configurazione dell'handler HTTP di default.
     *
     * @return la configurazione dell'handler, mai null
     */
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /** Property con prefisso {@code pipeline.web.exception-handler}. */
    public static class ExceptionHandler {

        /**
         * Attiva l'handler HTTP di default delle eccezioni della pipeline.
         */
        private boolean enabled = true;

        /**
         * Indica se l'handler HTTP di default è attivo.
         *
         * @return {@code true} se attivo (default)
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Attiva o disattiva l'handler HTTP di default.
         *
         * @param enabled {@code false} per non registrare {@code PipelineExceptionHandler}
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
