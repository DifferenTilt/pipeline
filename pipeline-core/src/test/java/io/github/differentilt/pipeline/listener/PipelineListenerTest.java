package io.github.differentilt.pipeline.listener;

import static io.github.differentilt.pipeline.TestContext.failing;
import static io.github.differentilt.pipeline.TestContext.tracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.differentilt.pipeline.DispatchRecordingListener;
import io.github.differentilt.pipeline.Pipeline;
import io.github.differentilt.pipeline.RecordingListener;
import io.github.differentilt.pipeline.TestContext;
import io.github.differentilt.pipeline.event.PipelineEvent;
import io.github.differentilt.pipeline.event.StepSkipped;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineListenerTest {

    @Test
    void listenerOverridingOnlyOnEventReceivesAllEvents() {
        RecordingListener<TestContext> listener = new RecordingListener<>();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .step("c", failing(new IllegalStateException("boom")))
                .activation((pipelineName, step) -> !step.equals("b"))
                .listener(listener)
                .build();

        catchThrowable(() -> pipeline.run(new TestContext()));

        assertThat(listener.descriptions()).containsExactly(
                "PipelineStarted",
                "StepStarted(a, 0)", "StepCompleted(a, 0)",
                "StepSkipped(b, 1)",
                "StepStarted(c, 2)", "StepFailed(c, 2)",
                "PipelineFailed(failedStep=c)");
    }

    @Test
    void listenerOverridingASpecificMethodReceivesOnlyThatEventType() {
        List<PipelineEvent<?>> received = new ArrayList<>();
        PipelineListener<TestContext> listener = new PipelineListener<>() {
            @Override
            public void onStepSkipped(StepSkipped<? extends TestContext> event) {
                received.add(event);
            }
        };
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .activation((pipelineName, step) -> !step.equals("b"))
                .listener(listener)
                .build();

        pipeline.run(new TestContext());

        assertThat(received).hasSize(1);
        assertThat(received.get(0)).isInstanceOf(StepSkipped.class);
        assertThat(RecordingListener.describe(received.get(0))).isEqualTo("StepSkipped(b, 1)");
    }

    @Test
    void defaultOnEventDispatchesEveryEventTypeToItsSpecificMethod() {
        List<String> calls = new ArrayList<>();
        PipelineListener<TestContext> listener = new DispatchRecordingListener(calls);
        Pipeline.Builder<TestContext> builder = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .activation((pipelineName, step) -> !step.equals("b"))
                .listener(listener);

        builder.build().run(new TestContext());
        catchThrowable(() -> builder.step("c", failing(new IllegalStateException("boom")))
                .build().run(new TestContext()));

        assertThat(calls).containsExactly(
                "pipelineStarted", "stepStarted", "stepCompleted", "stepSkipped", "pipelineCompleted",
                "pipelineStarted", "stepStarted", "stepCompleted", "stepSkipped",
                "stepStarted", "stepFailed", "pipelineFailed");
    }
}
