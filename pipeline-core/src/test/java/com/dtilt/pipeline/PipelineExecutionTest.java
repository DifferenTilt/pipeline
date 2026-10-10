package com.dtilt.pipeline;

import static com.dtilt.pipeline.TestContext.tracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dtilt.pipeline.step.StepActivation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class PipelineExecutionTest {

    private static Pipeline.Builder<TestContext> abc(String name) {
        return Pipeline.<TestContext>builder(name)
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .step("c", tracing("c"));
    }

    @Test
    void executesStepsInRegistrationOrder() {
        TestContext context = abc("p").build().run(new TestContext());

        assertThat(context.trace).containsExactly("a", "b", "c");
    }

    @Test
    void returnsTheSameContextInstance() {
        TestContext context = new TestContext();

        assertThat(abc("p").build().run(context)).isSameAs(context);
    }

    @Test
    void stepReadsValueWrittenByPreviousStep() {
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("write", context -> context.value = "hello")
                .step("read", context -> context.trace.add(context.value + " world"))
                .build();

        assertThat(pipeline.run(new TestContext()).trace).containsExactly("hello world");
    }

    @Test
    void runWithNullContextThrowsNullPointerException() {
        Pipeline<TestContext> pipeline = abc("p").build();

        assertThatThrownBy(() -> pipeline.run(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    void disabledStepIsSkippedAndOthersRun() {
        Pipeline<TestContext> pipeline = abc("p")
                .activation((pipelineName, step) -> !step.equals("b"))
                .build();

        assertThat(pipeline.run(new TestContext()).trace).containsExactly("a", "c");
    }

    @Test
    void activationReceivesPipelineAndStepNames() {
        List<String> calls = new ArrayList<>();
        Pipeline<TestContext> pipeline = abc("my-pipeline")
                .activation((pipelineName, step) -> {
                    calls.add(pipelineName + "/" + step);
                    return true;
                })
                .build();

        pipeline.run(new TestContext());

        assertThat(calls).containsExactly("my-pipeline/a", "my-pipeline/b", "my-pipeline/c");
    }

    @Test
    void activationIsEvaluatedOnEveryRun() {
        AtomicBoolean bEnabled = new AtomicBoolean(true);
        Pipeline<TestContext> pipeline = abc("p")
                .activation((pipelineName, step) -> !step.equals("b") || bEnabled.get())
                .build();

        assertThat(pipeline.run(new TestContext()).trace).containsExactly("a", "b", "c");

        bEnabled.set(false);

        assertThat(pipeline.run(new TestContext()).trace).containsExactly("a", "c");
    }

    @Test
    void requiredStepRunsEvenWhenTheActivationDisablesIt() {
        RecordingListener<TestContext> listener = new RecordingListener<>();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .requiredStep("b", tracing("b"))
                .step("c", tracing("c"))
                .activation((pipelineName, step) -> false)
                .listener(listener)
                .build();

        assertThat(pipeline.run(new TestContext()).trace).containsExactly("b");
        assertThat(listener.descriptions()).containsExactly(
                "PipelineStarted",
                "StepSkipped(a, 0)",
                "StepStarted(b, 1)",
                "StepCompleted(b, 1)",
                "StepSkipped(c, 2)",
                "PipelineCompleted(executed=[b], skipped=[a, c])");
    }

    @Test
    void activationIsNotConsultedForRequiredSteps() {
        List<String> calls = new ArrayList<>();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .requiredStep("b", tracing("b"))
                .activation((pipelineName, step) -> {
                    calls.add(step);
                    return true;
                })
                .build();

        pipeline.run(new TestContext());

        assertThat(calls).containsExactly("a");
    }

    @Test
    void disablingDisablesOnlyListedStepsOfListedPipeline() {
        StepActivation activation = StepActivation.disabling(Map.of("first", Set.of("b")));

        assertThat(abc("first").activation(activation).build().run(new TestContext()).trace)
                .containsExactly("a", "c");
        assertThat(abc("second").activation(activation).build().run(new TestContext()).trace)
                .containsExactly("a", "b", "c");
        assertThat(activation.isEnabled("first", "b")).isFalse();
        assertThat(activation.isEnabled("first", "a")).isTrue();
        assertThat(activation.isEnabled("second", "b")).isTrue();
        assertThat(activation.isEnabled("unknown", "unknown")).isTrue();
    }

    @Test
    void disablingCopiesTheMapDefensively() {
        Set<String> disabled = new HashSet<>(Set.of("b"));
        Map<String, Set<String>> source = new HashMap<>(Map.of("p", disabled));
        StepActivation activation = StepActivation.disabling(source);

        disabled.add("a");
        source.put("other", Set.of("a"));

        assertThat(activation.isEnabled("p", "a")).isTrue();
        assertThat(activation.isEnabled("p", "b")).isFalse();
        assertThat(activation.isEnabled("other", "a")).isTrue();
    }

    @Test
    void alwaysEnablesEveryStep() {
        assertThat(StepActivation.ALWAYS.isEnabled("any", "step")).isTrue();
    }

    @Test
    void factoryAppliesSharedActivationAndAllowsOverride() {
        PipelineFactory factory = PipelineFactory.builder()
                .activation(StepActivation.disabling(Map.of("p", Set.of("b"))))
                .build();

        Pipeline.Builder<TestContext> shared = factory.<TestContext>pipeline("p")
                .step("a", tracing("a")).step("b", tracing("b"));
        Pipeline.Builder<TestContext> overridden = factory.<TestContext>pipeline("p")
                .step("a", tracing("a")).step("b", tracing("b"))
                .activation(StepActivation.ALWAYS);

        assertThat(shared.build().run(new TestContext()).trace).containsExactly("a");
        assertThat(overridden.build().run(new TestContext()).trace).containsExactly("a", "b");
    }
}
