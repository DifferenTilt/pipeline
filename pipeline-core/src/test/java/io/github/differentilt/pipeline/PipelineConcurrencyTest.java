package io.github.differentilt.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PipelineConcurrencyTest {

    private static final int THREADS = 8;
    private static final int RUNS = 1000;

    private static final class Counter {
        final int input;
        int doubled;
        int result;
        final List<String> trace = new ArrayList<>();

        Counter(int input) {
            this.input = input;
        }
    }

    @Test
    void samePipelineRunsConcurrentlyOnDistinctContexts() throws Exception {
        RecordingListener<Counter> listener = new RecordingListener<>();
        Pipeline<Counter> pipeline = Pipeline.<Counter>builder("concurrent")
                .step("double", context -> {
                    context.doubled = context.input * 2;
                    context.trace.add("double");
                })
                .step("skipped", context -> context.trace.add("skipped"))
                .step("increment", context -> {
                    context.result = context.doubled + 1;
                    context.trace.add("increment");
                })
                .activation((pipelineName, step) -> !step.equals("skipped"))
                .listener(listener)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Counter> results = new ArrayList<>();
        try {
            List<Future<Counter>> futures = new ArrayList<>();
            for (int i = 0; i < RUNS; i++) {
                Counter context = new Counter(i);
                futures.add(executor.submit(() -> pipeline.run(context)));
            }
            for (Future<Counter> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(results).hasSize(RUNS);
        for (int i = 0; i < RUNS; i++) {
            Counter context = results.get(i);
            assertThat(context.input).isEqualTo(i);
            assertThat(context.result).isEqualTo(i * 2 + 1);
            assertThat(context.trace).containsExactly("double", "increment");
        }
        // 7 events per execution: start, 2 x (started + completed), 1 skipped, completion.
        assertThat(listener.events).hasSize(RUNS * 7);
        assertThat(listener.events.stream().map(event -> event.runId()).distinct()).hasSize(RUNS);
    }
}
