package com.dtilt.pipeline;

import static com.dtilt.pipeline.TestContext.failing;
import static com.dtilt.pipeline.TestContext.tracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.dtilt.pipeline.event.ListenerErrorPolicy;
import com.dtilt.pipeline.event.PipelineEvent;
import com.dtilt.pipeline.event.PipelineListener;
import com.dtilt.pipeline.event.StepCompleted;
import com.dtilt.pipeline.event.StepSkipped;
import com.dtilt.pipeline.exception.StepExecutionException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineListenerTest {

    /** Contesto usato solo per verificare la compatibilità dei tipi generici. */
    private static final class CreateProductContext {
    }

    private static <C> PipelineListener<C> named(String name, List<String> calls) {
        return new PipelineListener<>() {
            @Override
            public void onEvent(PipelineEvent<? extends C> event) {
                calls.add(name + ":" + event.getClass().getSimpleName());
            }
        };
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
    void factoryListenersAreNotifiedBeforePipelineListeners() {
        List<String> calls = new ArrayList<>();
        PipelineFactory factory = PipelineFactory.builder()
                .listener(named("global-1", calls))
                .listener(named("global-2", calls))
                .build();
        Pipeline<TestContext> pipeline = factory.<TestContext>pipeline("p")
                .listener(named("local", calls))
                .step("a", tracing("a"))
                .build();

        pipeline.run(new TestContext());

        assertThat(calls).startsWith(
                "global-1:PipelineStarted", "global-2:PipelineStarted", "local:PipelineStarted");
        assertThat(calls).endsWith(
                "global-1:PipelineCompleted", "global-2:PipelineCompleted", "local:PipelineCompleted");
    }

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
