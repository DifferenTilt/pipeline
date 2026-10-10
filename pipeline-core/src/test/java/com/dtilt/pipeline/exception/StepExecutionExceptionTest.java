package com.dtilt.pipeline.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StepExecutionExceptionTest {

    @Test
    void stepExecutionExceptionIsAlwaysLocated() {
        RuntimeException cause = new RuntimeException("why");

        StepExecutionException error = new StepExecutionException("p", "s", cause);
        error.locate("other", "other");

        assertThat(error.getPipeline()).isEqualTo("p");
        assertThat(error.getStep()).isEqualTo("s");
        assertThat(error).hasCause(cause).hasMessage("Step 's' of pipeline 'p' failed: why");
    }
}
