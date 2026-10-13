package io.github.differentilt.pipeline;

import io.github.differentilt.pipeline.event.PipelineCompleted;
import io.github.differentilt.pipeline.event.PipelineEvent;
import io.github.differentilt.pipeline.event.PipelineFailed;
import io.github.differentilt.pipeline.listener.PipelineListener;
import io.github.differentilt.pipeline.event.PipelineStarted;
import io.github.differentilt.pipeline.event.StepCompleted;
import io.github.differentilt.pipeline.event.StepFailed;
import io.github.differentilt.pipeline.event.StepSkipped;
import io.github.differentilt.pipeline.event.StepStarted;
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
