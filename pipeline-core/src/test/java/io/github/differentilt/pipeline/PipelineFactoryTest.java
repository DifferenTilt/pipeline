package io.github.differentilt.pipeline;

import static io.github.differentilt.pipeline.RecordingListener.named;
import static io.github.differentilt.pipeline.TestContext.tracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.differentilt.pipeline.step.StepActivation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PipelineFactoryTest {

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
}
