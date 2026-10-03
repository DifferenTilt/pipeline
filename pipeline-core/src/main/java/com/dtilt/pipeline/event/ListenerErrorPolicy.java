package com.dtilt.pipeline.event;

/** Comportamento della pipeline quando un {@link PipelineListener} lancia un'eccezione. */
public enum ListenerErrorPolicy {

    /**
     * L'eccezione del listener viene loggata a livello {@code WARNING}; gli altri listener vengono
     * comunque notificati e l'esecuzione della pipeline prosegue senza variazioni. È il default:
     * un listener rotto non deve mai cambiare l'esito di una pipeline.
     */
    LOG_AND_CONTINUE,

    /**
     * L'eccezione del listener si propaga subito al chiamante così com'è, la pipeline si
     * interrompe e non vengono emessi altri eventi. Pensata per i test.
     */
    PROPAGATE
}
