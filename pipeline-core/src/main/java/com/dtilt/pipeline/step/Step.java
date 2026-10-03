package com.dtilt.pipeline.step;

import com.dtilt.pipeline.Pipeline;
import com.dtilt.pipeline.exception.BusinessRuleException;

/**
 * A unit of business logic within a {@link Pipeline}.
 *
 * <p>It receives the execution context and can read and modify it: the context is the channel
 * through which a step passes results to the following steps. A step that must fail because of
 * a business rule throws {@link BusinessRuleException} (or a subclass of it).
 *
 * @param <C> the context type
 */
@FunctionalInterface
public interface Step<C> {

    /**
     * Executes the step logic.
     *
     * @param context the context of the current execution, never null
     */
    void execute(C context);
}
