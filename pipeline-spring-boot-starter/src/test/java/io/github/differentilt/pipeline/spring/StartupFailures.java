package io.github.differentilt.pipeline.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;

/** Support for tests that check that the context failed to start. */
final class StartupFailures {

    private StartupFailures() {
    }

    /** Checks that startup failed and returns the innermost cause of the failure. */
    static Throwable rootCause(ApplicationContextAssertProvider<?> context) {
        assertThat(context).hasFailed();
        Throwable cause = context.getStartupFailure();
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
