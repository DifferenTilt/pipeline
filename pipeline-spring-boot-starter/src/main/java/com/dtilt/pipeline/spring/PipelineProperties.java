package com.dtilt.pipeline.spring;

import com.dtilt.pipeline.listener.ListenerErrorPolicy;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurazione delle pipeline, letta dalle property con prefisso {@code pipeline}. */
@ConfigurationProperties("pipeline")
public class PipelineProperties {

    /**
     * Step disattivati, nel formato &lt;pipeline&gt;.&lt;step&gt; (ad esempio create-product.date-range).
     */
    private List<String> disabledSteps = new ArrayList<>();

    /**
     * Se true, le voci di disabled-steps che non corrispondono a pipeline o step esistenti
     * bloccano l'avvio; se false vengono solo loggate come WARN.
     */
    private boolean failOnUnknownSteps = true;

    /**
     * Comportamento quando un listener lancia un'eccezione.
     */
    private ListenerErrorPolicy listenerErrorPolicy = ListenerErrorPolicy.LOG_AND_CONTINUE;

    private final Logging logging = new Logging();

    /**
     * Step disattivati, nel formato {@code <pipeline>.<step>}.
     *
     * @return le voci configurate, mai null
     */
    public List<String> getDisabledSteps() {
        return disabledSteps;
    }

    /**
     * Imposta gli step disattivati.
     *
     * @param disabledSteps voci nel formato {@code <pipeline>.<step>}; null equivale a lista vuota
     */
    public void setDisabledSteps(List<String> disabledSteps) {
        this.disabledSteps = (disabledSteps != null) ? disabledSteps : new ArrayList<>();
    }

    /**
     * Indica se le voci sconosciute di {@code disabled-steps} bloccano l'avvio.
     *
     * @return {@code true} se bloccano l'avvio (default)
     */
    public boolean isFailOnUnknownSteps() {
        return failOnUnknownSteps;
    }

    /**
     * Imposta se le voci sconosciute di {@code disabled-steps} bloccano l'avvio.
     *
     * @param failOnUnknownSteps {@code true} per bloccare l'avvio, {@code false} per loggare un WARN
     */
    public void setFailOnUnknownSteps(boolean failOnUnknownSteps) {
        this.failOnUnknownSteps = failOnUnknownSteps;
    }

    /**
     * Policy di errore dei listener passata alla {@code PipelineFactory}.
     *
     * @return la policy, mai null
     */
    public ListenerErrorPolicy getListenerErrorPolicy() {
        return listenerErrorPolicy;
    }

    /**
     * Imposta la policy di errore dei listener.
     *
     * @param listenerErrorPolicy policy da usare; null equivale al default {@code LOG_AND_CONTINUE}
     */
    public void setListenerErrorPolicy(ListenerErrorPolicy listenerErrorPolicy) {
        this.listenerErrorPolicy =
                (listenerErrorPolicy != null) ? listenerErrorPolicy : ListenerErrorPolicy.LOG_AND_CONTINUE;
    }

    /**
     * Configurazione del logging delle pipeline.
     *
     * @return la configurazione del logging, mai null
     */
    public Logging getLogging() {
        return logging;
    }

    /** Property con prefisso {@code pipeline.logging}. */
    public static class Logging {

        /**
         * Registra LoggingPipelineListener come listener globale.
         */
        private boolean enabled = false;

        /**
         * Indica se {@code LoggingPipelineListener} viene registrato come listener globale.
         *
         * @return {@code true} se il listener di logging è attivo; default {@code false}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Attiva o disattiva il listener di logging.
         *
         * @param enabled {@code true} per registrare {@code LoggingPipelineListener}
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
