package com.dtilt.pipeline.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;

/** Supporto ai test che verificano il fallimento dell'avvio del contesto. */
final class StartupFailures {

    private StartupFailures() {
    }

    /** Verifica che l'avvio sia fallito e restituisce la causa più interna del fallimento. */
    static Throwable rootCause(AssertableApplicationContext context) {
        assertThat(context).hasFailed();
        Throwable cause = context.getStartupFailure();
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
