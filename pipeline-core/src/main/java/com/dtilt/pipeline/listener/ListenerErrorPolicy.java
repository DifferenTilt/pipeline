package com.dtilt.pipeline.listener;

/** What the pipeline does when a {@link PipelineListener} throws an exception. */
public enum ListenerErrorPolicy {

    /**
     * The listener's exception is logged at {@code WARNING}; the other listeners are still
     * notified and the pipeline carries on unchanged. This is the default: a broken listener
     * must never change the outcome of a pipeline.
     */
    LOG_AND_CONTINUE,

    /**
     * The listener's exception is propagated to the caller immediately and as is, the pipeline
     * stops and no further events are emitted. Intended for tests.
     */
    PROPAGATE
}
