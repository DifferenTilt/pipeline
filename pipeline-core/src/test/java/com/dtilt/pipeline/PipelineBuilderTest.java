package com.dtilt.pipeline;

import static com.dtilt.pipeline.TestContext.tracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PipelineBuilderTest {

    @Test
    void rejectsNullPipelineName() {
        assertThatThrownBy(() -> Pipeline.builder(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("pipeline name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "CreateProduct", "create_product", "create product",
            "-create", "create-", "create--product", "créer"})
    void rejectsBlankOrNonKebabCasePipelineName(String name) {
        assertThatThrownBy(() -> Pipeline.builder(name))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pipeline name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "create-product", "step-2", "42"})
    void acceptsKebabCaseNames(String name) {
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder(name)
                .step(name, tracing(name))
                .build();

        assertThat(pipeline.name()).isEqualTo(name);
        assertThat(pipeline.stepNames()).containsExactly(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "NameUnique", "name_unique", "name unique", "name-"})
    void rejectsNonKebabCaseStepName(String stepName) {
        Pipeline.Builder<TestContext> builder = Pipeline.builder("p");

        assertThatThrownBy(() -> builder.step(stepName, tracing("x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("step name");
    }

    @Test
    void rejectsNullStepName() {
        Pipeline.Builder<TestContext> builder = Pipeline.builder("p");

        assertThatThrownBy(() -> builder.step(null, tracing("x")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsDuplicateStepName() {
        Pipeline.Builder<TestContext> builder = Pipeline.<TestContext>builder("p").step("a", tracing("a"));

        assertThatThrownBy(() -> builder.step("a", tracing("other")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate")
                .hasMessageContaining("'a'");
    }

    @Test
    void requiredStepAppliesTheSameValidationAsStep() {
        Pipeline.Builder<TestContext> builder = Pipeline.<TestContext>builder("p").step("a", tracing("a"));

        assertThatThrownBy(() -> builder.requiredStep(null, tracing("x")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.requiredStep("b", null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("step");
        assertThatThrownBy(() -> builder.requiredStep("Not-Kebab", tracing("x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("step name");
        assertThatThrownBy(() -> builder.requiredStep("a", tracing("other")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate")
                .hasMessageContaining("'a'");
    }

    @Test
    void requiredStepNamesListOnlyRequiredStepsInOrderAndAreUnmodifiable() {
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .requiredStep("c", tracing("c"))
                .step("a", tracing("a"))
                .requiredStep("b", tracing("b"))
                .build();

        List<String> required = pipeline.requiredStepNames();

        assertThat(pipeline.stepNames()).containsExactly("c", "a", "b");
        assertThat(required).containsExactly("c", "b");
        assertThatThrownBy(() -> required.add("a")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiredStepNamesAreEmptyWhenNoStepIsRequired() {
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p").step("a", tracing("a")).build();

        assertThat(pipeline.requiredStepNames()).isEmpty();
    }

    @Test
    void rejectsNullArguments() {
        Pipeline.Builder<TestContext> builder = Pipeline.builder("p");

        assertThatThrownBy(() -> builder.step("a", null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("step");
        assertThatThrownBy(() -> builder.activation(null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("activation");
        assertThatThrownBy(() -> builder.listener(null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("listener");
        assertThatThrownBy(() -> builder.listenerErrorPolicy(null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("listenerErrorPolicy");
        assertThatThrownBy(() -> builder.clock(null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("clock");
        assertThatThrownBy(() -> builder.runIdSupplier(null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("runIdSupplier");
    }

    @Test
    void factoryBuilderRejectsNullArguments() {
        PipelineFactory.Builder builder = PipelineFactory.builder();

        assertThatThrownBy(() -> builder.activation(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.listener(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.listenerErrorPolicy(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.clock(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.build().pipeline("Not-Kebab"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildWithoutStepsThrowsIllegalStateException() {
        Pipeline.Builder<TestContext> builder = Pipeline.builder("p");

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least one step");
    }

    @Test
    void stepNamesFollowRegistrationOrderAndAreUnmodifiable() {
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("c", tracing("c"))
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .build();

        List<String> names = pipeline.stepNames();

        assertThat(names).containsExactly("c", "a", "b");
        assertThatThrownBy(() -> names.add("d")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void builderIsReusableWithoutAffectingAlreadyBuiltPipelines() {
        Pipeline.Builder<TestContext> builder = Pipeline.<TestContext>builder("p").step("a", tracing("a"));
        Pipeline<TestContext> first = builder.build();

        RecordingListener<TestContext> lateListener = new RecordingListener<>();
        Pipeline<TestContext> second = builder.step("b", tracing("b")).listener(lateListener).build();

        assertThat(first.stepNames()).containsExactly("a");
        assertThat(second.stepNames()).containsExactly("a", "b");
        assertThat(first.run(new TestContext()).trace).containsExactly("a");
        assertThat(lateListener.events).isEmpty();
        assertThat(second.run(new TestContext()).trace).containsExactly("a", "b");
        assertThat(lateListener.events).isNotEmpty();
    }
}
