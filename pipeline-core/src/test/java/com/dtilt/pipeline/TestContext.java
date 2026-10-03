package com.dtilt.pipeline;

import com.dtilt.pipeline.step.Step;
import java.util.ArrayList;
import java.util.List;

/** Test context: each step records its own name in {@link #trace}. */
final class TestContext {

    final List<String> trace = new ArrayList<>();
    String value;

    /** A step that records its own name in the context. */
    static Step<TestContext> tracing(String name) {
        return context -> context.trace.add(name);
    }

    /** A step that always throws the given exception. */
    static Step<TestContext> failing(RuntimeException error) {
        return context -> {
            throw error;
        };
    }
}
