package com.dtilt.pipeline.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.dtilt.pipeline.BusinessRuleException;
import com.dtilt.pipeline.BusinessRuleException.Category;
import com.dtilt.pipeline.Pipeline;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

class LoggingPipelineListenerTest {

    private record Entry(Level level, String message, Throwable thrown) {
    }

    private static final class CapturingLogger implements Logger {

        final List<Entry> entries = new ArrayList<>();

        @Override
        public String getName() {
            return LoggingPipelineListener.LOGGER_NAME;
        }

        @Override
        public boolean isLoggable(Level level) {
            return true;
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            entries.add(new Entry(level, msg, thrown));
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            entries.add(new Entry(level, format, null));
        }
    }

    private final CapturingLogger logger = new CapturingLogger();

    private Pipeline.Builder<StringBuilder> pipeline() {
        return Pipeline.<StringBuilder>builder("create-product")
                .runIdSupplier(() -> "run-1")
                .listener(new LoggingPipelineListener(logger));
    }

    private List<Level> levels() {
        return logger.entries.stream().map(Entry::level).toList();
    }

    @Test
    void usesTheModuleLoggerNameByDefault() {
        assertThat(LoggingPipelineListener.LOGGER_NAME).isEqualTo("com.dtilt.pipeline");
        assertThat(new LoggingPipelineListener()).isInstanceOf(PipelineListener.class);
    }

    @Test
    void logsSuccessfulRunAtDebugAndInfo() {
        pipeline()
                .step("a", context -> context.append("a"))
                .step("b", context -> context.append("b"))
                .activation((pipelineName, step) -> !step.equals("b"))
                .build()
                .run(new StringBuilder());

        assertThat(levels()).containsExactly(Level.DEBUG, Level.DEBUG, Level.DEBUG, Level.DEBUG, Level.INFO);
        assertThat(logger.entries).allSatisfy(entry -> {
            assertThat(entry.message()).contains("create-product").contains("run-1");
            assertThat(entry.thrown()).isNull();
        });
        assertThat(logger.entries.get(1).message()).contains("'a'").contains("started");
        assertThat(logger.entries.get(2).message()).contains("'a'").contains("completed in").contains("ms");
        assertThat(logger.entries.get(3).message()).contains("'b'").contains("skipped");
        assertThat(logger.entries.get(4).message())
                .contains("completed in").contains("1 step(s) executed").contains("1 skipped");
    }

    @Test
    void logsBusinessFailureAtInfoWithCodeAndWithoutStepFailedDuplicate() {
        Pipeline<StringBuilder> pipeline = pipeline()
                .step("name-unique", context -> {
                    throw new BusinessRuleException("PRODUCT_NAME_DUPLICATE", Category.CONFLICT, "duplicate");
                })
                .build();

        catchThrowable(() -> pipeline.run(new StringBuilder()));

        assertThat(levels()).containsExactly(Level.DEBUG, Level.DEBUG, Level.INFO);
        assertThat(logger.entries.get(2).message())
                .contains("create-product").contains("run-1")
                .contains("name-unique").contains("PRODUCT_NAME_DUPLICATE");
    }

    @Test
    void logsTechnicalFailureAtWarningWithMessageAndWithoutStackTrace() {
        Pipeline<StringBuilder> pipeline = pipeline()
                .step("save-product", context -> {
                    throw new IllegalStateException("db down");
                })
                .build();

        catchThrowable(() -> pipeline.run(new StringBuilder()));

        assertThat(levels()).containsExactly(Level.DEBUG, Level.DEBUG, Level.WARNING);
        Entry failure = logger.entries.get(2);
        assertThat(failure.message())
                .contains("create-product").contains("run-1").contains("save-product").contains("db down");
        assertThat(failure.thrown()).isNull();
    }
}
