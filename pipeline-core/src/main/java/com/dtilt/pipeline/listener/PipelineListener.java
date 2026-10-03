package com.dtilt.pipeline.listener;

import com.dtilt.pipeline.event.PipelineCompleted;
import com.dtilt.pipeline.event.PipelineEvent;
import com.dtilt.pipeline.event.PipelineFailed;
import com.dtilt.pipeline.event.PipelineStarted;
import com.dtilt.pipeline.event.StepCompleted;
import com.dtilt.pipeline.event.StepFailed;
import com.dtilt.pipeline.event.StepSkipped;
import com.dtilt.pipeline.event.StepStarted;

/**
 * Receives the lifecycle events of a pipeline, for logging, metrics and auditing.
 *
 * <p>The pipeline only calls {@link #onEvent(PipelineEvent)}. A listener can override
 * {@code onEvent} to handle every event in one place, or the individual specific methods.
 *
 * <p>Listeners receive a reference to the execution context and <strong>must not modify
 * it</strong>. A listener registered on a pipeline that is run by several threads must be
 * thread-safe.
 *
 * <p>The parameters use {@code ? extends C} on purpose: this lets a
 * {@code PipelineListener<Object>} be registered on a pipeline with any context type.
 *
 * @param <C> the type of the observed context
 */
public interface PipelineListener<C> {

    /**
     * The single entry point. The default dispatches the event to the specific method for its type.
     *
     * @param event the event emitted by the pipeline
     */
    default void onEvent(PipelineEvent<? extends C> event) {
        if (event instanceof PipelineStarted<? extends C> e) {
            onPipelineStarted(e);
        } else if (event instanceof StepStarted<? extends C> e) {
            onStepStarted(e);
        } else if (event instanceof StepSkipped<? extends C> e) {
            onStepSkipped(e);
        } else if (event instanceof StepCompleted<? extends C> e) {
            onStepCompleted(e);
        } else if (event instanceof StepFailed<? extends C> e) {
            onStepFailed(e);
        } else if (event instanceof PipelineCompleted<? extends C> e) {
            onPipelineCompleted(e);
        } else if (event instanceof PipelineFailed<? extends C> e) {
            onPipelineFailed(e);
        }
    }

    /**
     * Called when an execution starts.
     *
     * @param event the start event
     */
    default void onPipelineStarted(PipelineStarted<? extends C> event) {
    }

    /**
     * Called right before an enabled step is executed.
     *
     * @param event the step start event
     */
    default void onStepStarted(StepStarted<? extends C> event) {
    }

    /**
     * Called when a step is skipped because it is disabled.
     *
     * @param event the step skipped event
     */
    default void onStepSkipped(StepSkipped<? extends C> event) {
    }

    /**
     * Called when a step completes normally.
     *
     * @param event the step completion event
     */
    default void onStepCompleted(StepCompleted<? extends C> event) {
    }

    /**
     * Called when a step fails, before {@link #onPipelineFailed(PipelineFailed)}.
     *
     * @param event the step failure event
     */
    default void onStepFailed(StepFailed<? extends C> event) {
    }

    /**
     * Called when the execution finishes without errors.
     *
     * @param event the pipeline completion event
     */
    default void onPipelineCompleted(PipelineCompleted<? extends C> event) {
    }

    /**
     * Called when the execution stops because a step failed.
     *
     * @param event the pipeline failure event
     */
    default void onPipelineFailed(PipelineFailed<? extends C> event) {
    }
}
