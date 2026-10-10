package com.dtilt.pipeline.step;

import static com.dtilt.pipeline.TestContext.abc;
import static org.assertj.core.api.Assertions.assertThat;

import com.dtilt.pipeline.TestContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StepActivationTest {

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
}
