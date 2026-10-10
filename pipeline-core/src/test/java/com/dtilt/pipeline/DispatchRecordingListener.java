package com.dtilt.pipeline;

import com.dtilt.pipeline.event.PipelineCompleted;
import com.dtilt.pipeline.event.PipelineFailed;
import com.dtilt.pipeline.listener.PipelineListener;
import com.dtilt.pipeline.event.PipelineStarted;
import com.dtilt.pipeline.event.StepCompleted;
import com.dtilt.pipeline.event.StepFailed;
import com.dtilt.pipeline.event.StepSkipped;
import com.dtilt.pipeline.event.StepStarted;
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
