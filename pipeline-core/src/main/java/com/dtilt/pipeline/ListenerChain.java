package com.dtilt.pipeline;

import com.dtilt.pipeline.event.ListenerErrorPolicy;
import com.dtilt.pipeline.event.PipelineEvent;
import com.dtilt.pipeline.event.PipelineListener;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

/**
 * Notifica gli eventi ai listener nell'ordine di registrazione, applicando la
 * {@link ListenerErrorPolicy}. Immutabile e thread-safe.
 */
final class ListenerChain<C> {

    private static final Logger LOGGER = System.getLogger("com.dtilt.pipeline");

    private final List<PipelineListener<? super C>> listeners;
    private final ListenerErrorPolicy errorPolicy;

    ListenerChain(List<PipelineListener<? super C>> listeners, ListenerErrorPolicy errorPolicy) {
        this.listeners = List.copyOf(listeners);
        this.errorPolicy = errorPolicy;
    }

    void emit(PipelineEvent<C> event) {
        for (PipelineListener<? super C> listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                if (errorPolicy == ListenerErrorPolicy.PROPAGATE) {
                    throw e;
                }
                LOGGER.log(Level.WARNING, "Listener " + listener.getClass().getName()
                        + " failed handling " + event.getClass().getSimpleName()
                        + " of pipeline '" + event.pipeline() + "' [runId=" + event.runId() + "]", e);
            }
        }
    }
}
