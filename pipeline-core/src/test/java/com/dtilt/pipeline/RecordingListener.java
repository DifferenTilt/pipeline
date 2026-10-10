package com.dtilt.pipeline;

import com.dtilt.pipeline.event.PipelineCompleted;
import com.dtilt.pipeline.event.PipelineEvent;
import com.dtilt.pipeline.event.PipelineFailed;
import com.dtilt.pipeline.listener.PipelineListener;
import com.dtilt.pipeline.event.PipelineStarted;
import com.dtilt.pipeline.event.StepCompleted;
import com.dtilt.pipeline.event.StepFailed;
import com.dtilt.pipeline.event.StepSkipped;
import com.dtilt.pipeline.event.StepStarted;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test listener that collects every event received through {@code onEvent}. */
public final class RecordingListener<C> implements PipelineListener<C> {

    public final List<PipelineEvent<? extends C>> events = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(PipelineEvent<? extends C> event) {
        events.add(event);
    }

    /** A listener that appends {@code <name>:<event type>} to {@code calls} for every event. */
    public static <C> PipelineListener<C> named(String name, List<String> calls) {
        return new PipelineListener<>() {
            @Override
            public void onEvent(PipelineEvent<? extends C> event) {
                calls.add(name + ":" + event.getClass().getSimpleName());
            }
        };
    }

    /** Compact descriptions of the received events, in the order they were received. */
    public List<String> descriptions() {
        return events.stream().map(RecordingListener::describe).toList();
    }

    public static String describe(PipelineEvent<?> event) {
        if (event instanceof PipelineStarted<?>) {
            return "PipelineStarted";
        } else if (event instanceof StepStarted<?> e) {
            return "StepStarted(" + e.step() + ", " + e.stepIndex() + ")";
        } else if (event instanceof StepSkipped<?> e) {
            return "StepSkipped(" + e.step() + ", " + e.stepIndex() + ")";
        } else if (event instanceof StepCompleted<?> e) {
            return "StepCompleted(" + e.step() + ", " + e.stepIndex() + ")";
        } else if (event instanceof StepFailed<?> e) {
            return "StepFailed(" + e.step() + ", " + e.stepIndex() + ")";
        } else if (event instanceof PipelineCompleted<?> e) {
            return "PipelineCompleted(executed=" + e.executedSteps() + ", skipped=" + e.skippedSteps() + ")";
        } else if (event instanceof PipelineFailed<?> e) {
            return "PipelineFailed(failedStep=" + e.failedStep() + ")";
        }
        throw new IllegalArgumentException("unknown event: " + event);
    }
}
