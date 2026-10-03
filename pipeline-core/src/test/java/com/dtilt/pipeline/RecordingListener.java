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

/** Listener di test che raccoglie tutti gli eventi ricevuti tramite {@code onEvent}. */
final class RecordingListener<C> implements PipelineListener<C> {

    final List<PipelineEvent<? extends C>> events = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(PipelineEvent<? extends C> event) {
        events.add(event);
    }

    /** Descrizioni compatte degli eventi ricevuti, nell'ordine di ricezione. */
    List<String> descriptions() {
        return events.stream().map(RecordingListener::describe).toList();
    }

    static String describe(PipelineEvent<?> event) {
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
