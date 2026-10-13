package io.github.differentilt.pipeline;

import io.github.differentilt.pipeline.event.PipelineCompleted;
import io.github.differentilt.pipeline.event.PipelineFailed;
import io.github.differentilt.pipeline.listener.PipelineListener;
import io.github.differentilt.pipeline.event.PipelineStarted;
import io.github.differentilt.pipeline.event.StepCompleted;
import io.github.differentilt.pipeline.event.StepFailed;
import io.github.differentilt.pipeline.event.StepSkipped;
import io.github.differentilt.pipeline.event.StepStarted;
import java.util.List;

/** Test listener that overrides every specific method but not {@code onEvent}. */
public final class DispatchRecordingListener implements PipelineListener<TestContext> {

    private final List<String> calls;

    public DispatchRecordingListener(List<String> calls) {
        this.calls = calls;
    }

    @Override
    public void onPipelineStarted(PipelineStarted<? extends TestContext> event) {
        calls.add("pipelineStarted");
    }

    @Override
    public void onStepStarted(StepStarted<? extends TestContext> event) {
        calls.add("stepStarted");
    }

    @Override
    public void onStepSkipped(StepSkipped<? extends TestContext> event) {
        calls.add("stepSkipped");
    }

    @Override
    public void onStepCompleted(StepCompleted<? extends TestContext> event) {
        calls.add("stepCompleted");
    }

    @Override
    public void onStepFailed(StepFailed<? extends TestContext> event) {
        calls.add("stepFailed");
    }

    @Override
    public void onPipelineCompleted(PipelineCompleted<? extends TestContext> event) {
        calls.add("pipelineCompleted");
    }

    @Override
    public void onPipelineFailed(PipelineFailed<? extends TestContext> event) {
        calls.add("pipelineFailed");
    }
}
