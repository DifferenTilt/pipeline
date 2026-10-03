package com.dtilt.pipeline;

import static com.dtilt.pipeline.TestContext.failing;
import static com.dtilt.pipeline.TestContext.tracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.dtilt.pipeline.BusinessRuleException.Category;
import com.dtilt.pipeline.event.PipelineCompleted;
import com.dtilt.pipeline.event.PipelineEvent;
import com.dtilt.pipeline.event.PipelineFailed;
import com.dtilt.pipeline.event.StepCompleted;
import com.dtilt.pipeline.event.StepFailed;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PipelineEventTest {

    private final RecordingListener<TestContext> listener = new RecordingListener<>();

    private Pipeline.Builder<TestContext> abcWithBDisabled() {
        return Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .step("c", tracing("c"))
                .activation(StepActivation.disabling(Map.of("p", Set.of("b"))))
                .listener(listener);
    }

    private Pipeline<TestContext> abcWithBFailing(RuntimeException error) {
        return Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .step("b", failing(error))
                .step("c", tracing("c"))
                .listener(listener)
                .build();
    }

    private static List<Duration> durations(List<? extends PipelineEvent<?>> events) {
        List<Duration> durations = new ArrayList<>();
        for (PipelineEvent<?> event : events) {
            if (event instanceof StepCompleted<?> e) {
                durations.add(e.duration());
            } else if (event instanceof StepFailed<?> e) {
                durations.add(e.duration());
            } else if (event instanceof PipelineCompleted<?> e) {
                durations.add(e.duration());
            } else if (event instanceof PipelineFailed<?> e) {
                durations.add(e.duration());
            }
        }
        return durations;
    }

    @Test
    void sequenceForSuccessfulRunWithSkippedStep() {
        abcWithBDisabled().build().run(new TestContext());

        assertThat(listener.descriptions()).containsExactly(
                "PipelineStarted",
                "StepStarted(a, 0)", "StepCompleted(a, 0)",
                "StepSkipped(b, 1)",
                "StepStarted(c, 2)", "StepCompleted(c, 2)",
                "PipelineCompleted(executed=[a, c], skipped=[b])");
    }

    @Test
    void sequenceWhenAStepFails() {
        Pipeline<TestContext> pipeline = abcWithBFailing(new IllegalStateException("boom"));

        assertThatThrownBy(() -> pipeline.run(new TestContext())).isInstanceOf(StepExecutionException.class);

        assertThat(listener.descriptions()).containsExactly(
                "PipelineStarted",
                "StepStarted(a, 0)", "StepCompleted(a, 0)",
                "StepStarted(b, 1)", "StepFailed(b, 1)",
                "PipelineFailed(failedStep=b)");
    }

    @Test
    void sequenceWhenActivationFails() {
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .activation((pipelineName, step) -> {
                    if (step.equals("b")) {
                        throw new IllegalStateException("boom");
                    }
                    return true;
                })
                .listener(listener)
                .build();

        assertThatThrownBy(() -> pipeline.run(new TestContext())).isInstanceOf(StepExecutionException.class);

        assertThat(listener.descriptions()).containsExactly(
                "PipelineStarted",
                "StepStarted(a, 0)", "StepCompleted(a, 0)",
                "StepFailed(b, 1)",
                "PipelineFailed(failedStep=b)");
        StepFailed<?> stepFailed = (StepFailed<?>) listener.events.get(3);
        assertThat(stepFailed.duration()).isEqualTo(Duration.ZERO);
    }

    @Test
    void eventsCarryPipelineNameAndContext() {
        TestContext context = new TestContext();

        abcWithBDisabled().build().run(context);

        assertThat(listener.events).allSatisfy(event -> {
            assertThat(event.pipeline()).isEqualTo("p");
            assertThat(event.context()).isSameAs(context);
        });
    }

    @Test
    void allEventsOfARunShareTheRunIdAndRunsHaveDifferentRunIds() {
        Pipeline<TestContext> pipeline = abcWithBDisabled().build();

        pipeline.run(new TestContext());
        List<String> firstRun = listener.events.stream().map(PipelineEvent::runId).distinct().toList();
        listener.events.clear();
        pipeline.run(new TestContext());
        List<String> secondRun = listener.events.stream().map(PipelineEvent::runId).distinct().toList();

        assertThat(firstRun).hasSize(1);
        assertThat(secondRun).hasSize(1);
        assertThat(firstRun.get(0)).isNotBlank().isNotEqualTo(secondRun.get(0));
    }

    @Test
    void customRunIdSupplierProvidesTheRunId() {
        AtomicInteger counter = new AtomicInteger();
        Pipeline<TestContext> pipeline = abcWithBDisabled()
                .runIdSupplier(() -> "run-" + counter.incrementAndGet())
                .build();

        pipeline.run(new TestContext());

        assertThat(listener.events).isNotEmpty().allSatisfy(event -> assertThat(event.runId()).isEqualTo("run-1"));
        assertThat(counter).hasValue(1);
    }

    @Test
    void occurredAtComesFromTheConfiguredClock() {
        Instant fixed = Instant.parse("2026-01-02T03:04:05Z");
        Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);

        abcWithBDisabled().clock(clock).build().run(new TestContext());
        Pipeline<TestContext> failing = Pipeline.<TestContext>builder("f")
                .step("a", failing(new IllegalStateException("boom")))
                .listener(listener)
                .clock(clock)
                .build();
        catchThrowable(() -> failing.run(new TestContext()));

        assertThat(listener.events).hasSize(11)
                .allSatisfy(event -> assertThat(event.occurredAt()).isEqualTo(fixed));
    }

    @Test
    void durationsAreNeverNegative() {
        abcWithBDisabled().build().run(new TestContext());
        catchThrowable(() -> abcWithBFailing(new IllegalStateException("boom")).run(new TestContext()));

        assertThat(durations(listener.events)).hasSize(6)
                .allSatisfy(duration -> assertThat(duration).isGreaterThanOrEqualTo(Duration.ZERO));
    }

    @Test
    void failureEventsCarryTheSameInstanceThrownToTheCaller() {
        Throwable wrapped = catchThrowable(
                () -> abcWithBFailing(new IllegalStateException("boom")).run(new TestContext()));

        assertFailureEventsCarry(wrapped);
        assertThat(wrapped).isInstanceOf(StepExecutionException.class);

        listener.events.clear();
        BusinessRuleException businessError = new BusinessRuleException("CODE", Category.NOT_FOUND, "missing");
        Throwable business = catchThrowable(() -> abcWithBFailing(businessError).run(new TestContext()));

        assertThat(business).isSameAs(businessError);
        assertFailureEventsCarry(business);
    }

    private void assertFailureEventsCarry(Throwable thrown) {
        StepFailed<?> stepFailed = (StepFailed<?>) listener.events.get(listener.events.size() - 2);
        PipelineFailed<?> pipelineFailed = (PipelineFailed<?>) listener.events.get(listener.events.size() - 1);
        assertThat(stepFailed.error()).isSameAs(thrown);
        assertThat(pipelineFailed.error()).isSameAs(thrown);
        assertThat(pipelineFailed.error().getStep()).isEqualTo("b");
        assertThat(pipelineFailed.failedStep()).isEqualTo("b");
    }

    @Test
    void pipelineCompletedReportsExecutedAndSkippedSteps() {
        abcWithBDisabled().build().run(new TestContext());

        PipelineCompleted<?> completed = (PipelineCompleted<?>) listener.events.get(listener.events.size() - 1);

        assertThat(completed.executedSteps()).containsExactly("a", "c");
        assertThat(completed.skippedSteps()).containsExactly("b");
        assertThatThrownBy(() -> completed.executedSteps().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> completed.skippedSteps().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
