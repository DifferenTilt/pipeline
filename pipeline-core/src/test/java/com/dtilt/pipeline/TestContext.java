package com.dtilt.pipeline;

import java.util.ArrayList;
import java.util.List;

/** Contesto di test: ogni step registra il proprio nome in {@link #trace}. */
final class TestContext {

    final List<String> trace = new ArrayList<>();
    String value;

    /** Step che registra il proprio nome nel contesto. */
    static Step<TestContext> tracing(String name) {
        return context -> context.trace.add(name);
    }

    /** Step che lancia sempre l'eccezione indicata. */
    static Step<TestContext> failing(RuntimeException error) {
        return context -> {
            throw error;
        };
    }
}
