package io.github.differentilt.pipeline;

import static io.github.differentilt.pipeline.RecordingListener.named;
import static io.github.differentilt.pipeline.TestContext.failing;
import static io.github.differentilt.pipeline.TestContext.tracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.differentilt.pipeline.event.PipelineEvent;
import io.github.differentilt.pipeline.event.StepCompleted;
import io.github.differentilt.pipeline.exception.StepExecutionException;
import io.github.differentilt.pipeline.listener.ListenerErrorPolicy;
import io.github.differentilt.pipeline.listener.PipelineListener;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link ListenerChain} is exercised through {@link Pipeline}, which is its only caller. */
class ListenerChainTest {

    /** Context used only to check the compatibility of generic types. */
    private static final class CreateProductContext {
    }

    private static PipelineListener<Object> broken(RuntimeException error) {
        return new PipelineListener<>() {
            @Override
            public void onEvent(PipelineEvent<?> event) {
                throw error;
            }
        };
    }

    @Test
    void listenersAreNotifiedInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .listener(named("first", calls))
                .listener(named("second", calls))
                .listener(named("third", calls))
                .build();

        pipeline.run(new TestContext());

        assertThat(calls).containsExactly(
                "first:PipelineStarted", "second:PipelineStarted", "third:PipelineStarted",
                "first:StepStarted", "second:StepStarted", "third:StepStarted",
                "first:StepCompleted", "second:StepCompleted", "third:StepCompleted",
                "first:PipelineCompleted", "second:PipelineCompleted", "third:PipelineCompleted");
    }

    @Test
    void objectListenerCanBeRegisteredOnATypedPipeline() {
        RecordingListener<Object> listener = new RecordingListener<>();
        PipelineListener<Object> objectListener = listener;

        Pipeline<CreateProductContext> pipeline = Pipeline.<CreateProductContext>builder("create-product")
                .step("noop", context -> { })
                .listener(objectListener)
                .build();
        pipeline.run(new CreateProductContext());

        assertThat(listener.events).hasSize(4);
    }

    @Test
    void logAndContinueKeepsOutcomeAndNotifiesOtherListeners() {
        RecordingListener<TestContext> before = new RecordingListener<>();
        RecordingListener<TestContext> after = new RecordingListener<>();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .listener(before)
                .listener(broken(new IllegalStateException("listener is broken")))
                .listener(after)
                .build();

        TestContext context = pipeline.run(new TestContext());

        assertThat(context.trace).containsExactly("a", "b");
        assertThat(before.events).hasSize(6);
        assertThat(after.descriptions()).isEqualTo(before.descriptions());
    }

    @Test
    void logAndContinueDoesNotAlterTheExceptionOfAFailingPipeline() {
        IllegalStateException cause = new IllegalStateException("boom");
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", failing(cause))
                .listener(broken(new IllegalStateException("listener is broken")))
                .build();

        Throwable thrown = catchThrowable(() -> pipeline.run(new TestContext()));

        assertThat(thrown).isInstanceOf(StepExecutionException.class).hasCause(cause);
    }

    @Test
    void propagateLetsTheListenerExceptionReachTheCaller() {
        IllegalStateException listenerError = new IllegalStateException("listener is broken");
        RecordingListener<TestContext> after = new RecordingListener<>();
        TestContext context = new TestContext();
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .listener(broken(listenerError))
                .listener(after)
                .listenerErrorPolicy(ListenerErrorPolicy.PROPAGATE)
                .build();

        Throwable thrown = catchThrowable(() -> pipeline.run(context));

        assertThat(thrown).isSameAs(listenerError);
        assertThat(after.events).isEmpty();
        assertThat(context.trace).isEmpty();
    }

    @Test
    void propagateDoesNotTreatAListenerFailureAsAStepFailure() {
        IllegalStateException listenerError = new IllegalStateException("listener is broken");
        RecordingListener<TestContext> before = new RecordingListener<>();
        PipelineListener<TestContext> failingOnCompletion = new PipelineListener<>() {
            @Override
            public void onStepCompleted(StepCompleted<? extends TestContext> event) {
                throw listenerError;
            }
        };
        Pipeline<TestContext> pipeline = Pipeline.<TestContext>builder("p")
                .step("a", tracing("a"))
                .listener(before)
                .listener(failingOnCompletion)
                .listenerErrorPolicy(ListenerErrorPolicy.PROPAGATE)
                .build();

        Throwable thrown = catchThrowable(() -> pipeline.run(new TestContext()));

        assertThat(thrown).isSameAs(listenerError);
        assertThat(before.descriptions())
                .containsExactly("PipelineStarted", "StepStarted(a, 0)", "StepCompleted(a, 0)");
    }
}
