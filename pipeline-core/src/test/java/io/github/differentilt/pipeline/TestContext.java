package io.github.differentilt.pipeline;

import io.github.differentilt.pipeline.step.Step;
import java.util.ArrayList;
import java.util.List;

/** Test context: each step records its own name in {@link #trace}. */
public final class TestContext {

    public final List<String> trace = new ArrayList<>();
    public String value;

    /** A step that records its own name in the context. */
    public static Step<TestContext> tracing(String name) {
        return context -> context.trace.add(name);
    }

    /** A step that always throws the given exception. */
    public static Step<TestContext> failing(RuntimeException error) {
        return context -> {
            throw error;
        };
    }

    /** A builder for a pipeline with the three tracing steps {@code a}, {@code b} and {@code c}. */
    public static Pipeline.Builder<TestContext> abc(String name) {
        return Pipeline.<TestContext>builder(name)
                .step("a", tracing("a"))
                .step("b", tracing("b"))
                .step("c", tracing("c"));
    }
}
