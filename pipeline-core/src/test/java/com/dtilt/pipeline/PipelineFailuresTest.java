package com.dtilt.pipeline;

import static com.dtilt.pipeline.TestContext.failing;
import static com.dtilt.pipeline.TestContext.tracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.dtilt.pipeline.exception.BusinessRuleException;
import com.dtilt.pipeline.exception.BusinessRuleException.Category;
import com.dtilt.pipeline.event.PipelineFailed;
import com.dtilt.pipeline.event.StepFailed;
import com.dtilt.pipeline.exception.StepExecutionException;
import org.junit.jupiter.api.Test;

class PipelineFailuresTest {

    private static BusinessRuleException businessError() {
        return new BusinessRuleException("PRODUCT_NAME_DUPLICATE", Category.CONFLICT, "duplicate name");
    }

    @Test
    void businessRuleExceptionIsPropagatedAsSameInstanceAndLocated() {
        BusinessRuleException error = businessError();
        TestContext context = new TestContext();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("create-product")
                .step("a", tracing("a"))
                .step("name-unique", failing(error))
                .step("c", tracing("c"))
                .build();

        Throwable thrown = catchThrowable(() -> pipeline.run(context));

        assertThat(thrown).isSameAs(error);
        assertThat(error.getPipeline()).isEqualTo("create-product");
        assertThat(error.getStep()).isEqualTo("name-unique");
        assertThat(context.trace).containsExactly("a");
    }

    @Test
    void genericRuntimeExceptionIsWrappedInStepExecutionException() {
        IllegalStateException cause = new IllegalStateException("db down");
        TestContext context = new TestContext();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("create-product")
                .step("save-product", failing(cause))
                .step("after", tracing("after"))
                .build();

        Throwable thrown = catchThrowable(() -> pipeline.run(context));

        assertThat(thrown).isExactlyInstanceOf(StepExecutionException.class)
                .hasCause(cause)
                .hasMessage("Step 'save-product' of pipeline 'create-product' failed: db down");
        StepExecutionException error = (StepExecutionException) thrown;
        assertThat(error.getPipeline()).isEqualTo("create-product");
        assertThat(error.getStep()).isEqualTo("save-product");
        assertThat(context.trace).isEmpty();
    }

    @Test
    void nestedPipelineKeepsInnermostLocationForBusinessErrors() {
        BusinessRuleException error = businessError();
        Pipeline<TestContext> inner = Pipeline.<TestContext>builder("inner")
                .step("inner-step", failing(error))
                .build();
        Pipeline<TestContext> outer = Pipeline.<TestContext>builder("outer")
                .step("outer-step", inner::run)
                .build();

        Throwable thrown = catchThrowable(() -> outer.run(new TestContext()));

        assertThat(thrown).isSameAs(error);
        assertThat(error.getPipeline()).isEqualTo("inner");
        assertThat(error.getStep()).isEqualTo("inner-step");
    }

    @Test
    void nestedPipelineDoesNotRewrapStepExecutionException() {
        IllegalStateException cause = new IllegalStateException("boom");
        Pipeline<TestContext> inner = Pipeline.<TestContext>builder("inner")
                .step("inner-step", failing(cause))
                .build();
        Pipeline<TestContext> outer = Pipeline.<TestContext>builder("outer")
                .step("outer-step", inner::run)
                .build();

        Throwable thrown = catchThrowable(() -> outer.run(new TestContext()));

        assertThat(thrown).isExactlyInstanceOf(StepExecutionException.class).hasCause(cause);
        StepExecutionException error = (StepExecutionException) thrown;
        assertThat(error.getPipeline()).isEqualTo("inner");
        assertThat(error.getStep()).isEqualTo("inner-step");
    }

    @Test
    void activationFailureIsPropagatedAsStepExecutionExceptionOnThatStep() {
        IllegalStateException cause = new IllegalStateException("config unavailable");
        TestContext context = new TestContext();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .activation((pipelineName, step) -> {
                    if (step.equals("b")) {
                        throw cause;
                    }
                    return true;
                })
                .build();

        Throwable thrown = catchThrowable(() -> pipeline.run(context));

        assertThat(thrown).isExactlyInstanceOf(StepExecutionException.class).hasCause(cause);
        StepExecutionException error = (StepExecutionException) thrown;
        assertThat(error.getPipeline()).isEqualTo("p");
        assertThat(error.getStep()).isEqualTo("b");
        assertThat(context.trace).containsExactly("a");
    }

    @Test
    void errorPropagatesUnchangedWithoutFailureEvents() {
        Error error = new Error("fatal");
        RecordingListener<TestContext> listener = new RecordingListener<>();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", context -> {
                    throw error;
                })
                .listener(listener)
                .build();

        Throwable thrown = catchThrowable(() -> pipeline.run(new TestContext()));

        assertThat(thrown).isSameAs(error);
        assertThat(listener.events)
                .noneMatch(event -> event instanceof StepFailed<?> || event instanceof PipelineFailed<?>);
        assertThat(listener.descriptions()).containsExactly("PipelineStarted", "StepStarted(a, 0)");
    }
}
