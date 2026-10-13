package io.github.differentilt.pipeline;

import io.github.differentilt.pipeline.listener.ListenerErrorPolicy;
import io.github.differentilt.pipeline.event.PipelineEvent;
import io.github.differentilt.pipeline.listener.PipelineListener;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

/**
 * Notifies listeners of events in registration order, applying the
 * {@link ListenerErrorPolicy}. Immutable and thread-safe.
 */
final class ListenerChain<C> {

    private static final Logger LOGGER = System.getLogger("io.github.differentilt.pipeline");

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
