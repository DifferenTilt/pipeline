package io.github.differentilt.pipeline;

import io.github.differentilt.pipeline.listener.ListenerErrorPolicy;
import io.github.differentilt.pipeline.event.PipelineCompleted;
import io.github.differentilt.pipeline.event.PipelineFailed;
import io.github.differentilt.pipeline.listener.PipelineListener;
import io.github.differentilt.pipeline.event.PipelineStarted;
import io.github.differentilt.pipeline.event.StepCompleted;
import io.github.differentilt.pipeline.event.StepFailed;
import io.github.differentilt.pipeline.event.StepSkipped;
import io.github.differentilt.pipeline.event.StepStarted;
import io.github.differentilt.pipeline.exception.PipelineException;
import io.github.differentilt.pipeline.exception.StepExecutionException;
import io.github.differentilt.pipeline.step.Step;
import io.github.differentilt.pipeline.step.StepActivation;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An ordered, immutable sequence of named {@link Step}s, executed with {@link #run(Object)}.
 *
 * <p>Execution is fail-fast: the first step that throws stops the pipeline, and the exception
 * is propagated to the caller as a {@link PipelineException}.
 *
 * <p>{@code Pipeline} is immutable and thread-safe: the same instance can execute {@code run}
 * concurrently on different contexts. The context is not shared between executions and does not
 * need to be thread-safe.
 *
 * @param <C> the context type
 */
public final class Pipeline<C> {

    private final String name;
    private final List<NamedStep<C>> steps;
    private final List<String> stepNames;
    private final List<String> requiredStepNames;
    private final StepActivation activation;
    private final ListenerChain<C> listeners;
    private final Clock clock;
    private final Supplier<String> runIdSupplier;

    private Pipeline(Builder<C> builder) {
        this.name = builder.name;
        this.steps = List.copyOf(builder.steps);
        this.stepNames = this.steps.stream().map(NamedStep::name).toList();
        this.requiredStepNames = this.steps.stream().filter(NamedStep::required).map(NamedStep::name).toList();
        this.activation = builder.activation;
        this.listeners = new ListenerChain<>(builder.listeners, builder.listenerErrorPolicy);
        this.clock = builder.clock;
        this.runIdSupplier = builder.runIdSupplier;
    }

    /**
     * Creates a builder for a pipeline with the given name.
     *
     * @param name the pipeline name, in kebab-case ({@code ^[a-z0-9]+(-[a-z0-9]+)*$})
     * @param <C>  the context type
     * @return a new builder
     * @throws NullPointerException     if {@code name} is null
     * @throws IllegalArgumentException if {@code name} is not kebab-case
     */
    public static <C> Builder<C> builder(String name) {
        return new Builder<>(Names.requireKebabCase(name, "pipeline name"));
    }

    /**
     * The pipeline name.
     *
     * @return the pipeline name
     */
    public String name() {
        return name;
    }

    /**
     * The step names, in execution order.
     *
     * @return an unmodifiable list of the step names
     */
    public List<String> stepNames() {
        return stepNames;
    }

    /**
     * The names of the required steps, in execution order. A required step is always executed:
     * the {@link StepActivation} cannot disable it.
     *
     * @return an unmodifiable list of the required step names
     * @see Builder#requiredStep(String, Step)
     */
    public List<String> requiredStepNames() {
        return requiredStepNames;
    }

    /**
     * Runs the pipeline: evaluates the activation of each step, in order, and executes the
     * enabled ones. Required steps are always executed, without consulting the activation.
     *
     * @param context the execution context; not null
     * @return the same context instance that was passed in
     * @throws NullPointerException   if {@code context} is null
     * @throws PipelineException      if a step throws a {@code PipelineException} (propagated as
     *                                the same instance, with its location set)
     * @throws StepExecutionException if a step, or the evaluation of its activation, throws any
     *                                other {@link RuntimeException}
     */
    public C run(C context) {
        Objects.requireNonNull(context, "context must not be null");
        String runId = Objects.requireNonNull(runIdSupplier.get(), "runIdSupplier returned null");
        return new Execution(runId, context).run();
    }

    private static Duration elapsedSince(long startNanos) {
        return Duration.ofNanos(Math.max(0L, System.nanoTime() - startNanos));
    }

    private record NamedStep<C>(String name, Step<C> step, boolean required) {
    }

    /** A single execution of the pipeline: holds the state of the run and emits its events. */
    private final class Execution {

        private final String runId;
        private final C context;
        private final long startNanos;
        private final List<String> executedSteps = new ArrayList<>();
        private final List<String> skippedSteps = new ArrayList<>();

        private Execution(String runId, C context) {
            this.runId = runId;
            this.context = context;
            this.startNanos = System.nanoTime();
        }

        private C run() {
            listeners.emit(new PipelineStarted<>(name, runId, clock.instant(), context));
            for (int i = 0; i < steps.size(); i++) {
                runStep(steps.get(i), i);
            }
            listeners.emit(new PipelineCompleted<>(name, runId, clock.instant(), context,
                    elapsedSince(startNanos), executedSteps, skippedSteps));
            return context;
        }

        private void runStep(NamedStep<C> step, int index) {
            if (isEnabled(step, index)) {
                execute(step, index);
                executedSteps.add(step.name());
            } else {
                listeners.emit(new StepSkipped<>(name, runId, clock.instant(), context, step.name(), index));
                skippedSteps.add(step.name());
            }
        }

        private boolean isEnabled(NamedStep<C> step, int index) {
            try {
                return step.required() || activation.isEnabled(name, step.name());
            } catch (RuntimeException e) {
                throw fail(step, index, Duration.ZERO, e);
            }
        }

        private void execute(NamedStep<C> step, int index) {
            listeners.emit(new StepStarted<>(name, runId, clock.instant(), context, step.name(), index));
            long stepStart = System.nanoTime();
            try {
                step.step().execute(context);
            } catch (RuntimeException e) {
                throw fail(step, index, elapsedSince(stepStart), e);
            }
            listeners.emit(new StepCompleted<>(name, runId, clock.instant(), context, step.name(), index,
                    elapsedSince(stepStart)));
        }

        /** Translates the exception, emits StepFailed and PipelineFailed, and returns the exception to throw. */
        private PipelineException fail(NamedStep<C> step, int index, Duration stepDuration, RuntimeException e) {
            PipelineException error = translate(step.name(), e);
            listeners.emit(new StepFailed<>(name, runId, clock.instant(), context, step.name(), index,
                    stepDuration, error));
            listeners.emit(new PipelineFailed<>(name, runId, clock.instant(), context,
                    elapsedSince(startNanos), step.name(), error));
            return error;
        }

        private PipelineException translate(String stepName, RuntimeException e) {
            if (e instanceof PipelineException pipelineException) {
                pipelineException.locate(name, stepName);
                return pipelineException;
            }
            return new StepExecutionException(name, stepName, e);
        }
    }

    /**
     * Builder for {@link Pipeline}. Not thread-safe. It can be reused: {@link #build()} copies
     * the lists, so later changes do not affect pipelines that were already built.
     *
     * @param <C> the context type
     */
    public static final class Builder<C> {

        private final String name;
        private final List<NamedStep<C>> steps = new ArrayList<>();
        private final List<PipelineListener<? super C>> listeners = new ArrayList<>();
        private StepActivation activation = StepActivation.ALWAYS;
        private ListenerErrorPolicy listenerErrorPolicy = ListenerErrorPolicy.LOG_AND_CONTINUE;
        private Clock clock = Clock.systemUTC();
        private Supplier<String> runIdSupplier = () -> UUID.randomUUID().toString();

        private Builder(String name) {
            this.name = name;
        }

        /**
         * Appends a step to the pipeline. The step can be disabled by the {@link StepActivation}.
         *
         * @param name the step name, in kebab-case and unique within the pipeline
         * @param step the step logic; not null
         * @return this builder
         * @throws NullPointerException     if {@code name} or {@code step} is null
         * @throws IllegalArgumentException if {@code name} is not kebab-case or is already used
         */
        public Builder<C> step(String name, Step<C> step) {
            return addStep(name, step, false);
        }

        /**
         * Appends a required step to the pipeline. A required step is always executed: the
         * {@link StepActivation} is not consulted for it, so it can never be disabled. Use it
         * for the steps the pipeline cannot do without, such as the one that persists the data
         * or produces the result read by the caller.
         *
         * @param name the step name, in kebab-case and unique within the pipeline
         * @param step the step logic; not null
         * @return this builder
         * @throws NullPointerException     if {@code name} or {@code step} is null
         * @throws IllegalArgumentException if {@code name} is not kebab-case or is already used
         */
        public Builder<C> requiredStep(String name, Step<C> step) {
            return addStep(name, step, true);
        }

        private Builder<C> addStep(String name, Step<C> step, boolean required) {
            Names.requireKebabCase(name, "step name");
            Objects.requireNonNull(step, "step must not be null");
            if (steps.stream().anyMatch(existing -> existing.name().equals(name))) {
                throw new IllegalArgumentException(
                        "duplicate step name '" + name + "' in pipeline '" + this.name + "'");
            }
            steps.add(new NamedStep<>(name, step, required));
            return this;
        }

        /**
         * Sets the step activation. Default: {@link StepActivation#ALWAYS}.
         *
         * @param activation the activation; not null
         * @return this builder
         * @throws NullPointerException if {@code activation} is null
         */
        public Builder<C> activation(StepActivation activation) {
            this.activation = Objects.requireNonNull(activation, "activation must not be null");
            return this;
        }

        /**
         * Registers a listener. It can be called several times: listeners are notified in
         * registration order.
         *
         * @param listener the listener to register; not null
         * @return this builder
         * @throws NullPointerException if {@code listener} is null
         */
        public Builder<C> listener(PipelineListener<? super C> listener) {
            listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
            return this;
        }

        /**
         * Sets what happens when a listener throws an exception.
         * Default: {@link ListenerErrorPolicy#LOG_AND_CONTINUE}.
         *
         * @param policy the policy to apply; not null
         * @return this builder
         * @throws NullPointerException if {@code policy} is null
         */
        public Builder<C> listenerErrorPolicy(ListenerErrorPolicy policy) {
            this.listenerErrorPolicy = Objects.requireNonNull(policy, "listenerErrorPolicy must not be null");
            return this;
        }

        /**
         * Sets the clock from which the {@code occurredAt} of events is read.
         * Default: {@link Clock#systemUTC()}.
         *
         * @param clock the clock to use; not null
         * @return this builder
         * @throws NullPointerException if {@code clock} is null
         */
        public Builder<C> clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
            return this;
        }

        /**
         * Sets the supplier of run identifiers. Default: a random UUID.
         *
         * @param runIdSupplier the supplier, invoked once per execution; not null and must not
         *                      return null
         * @return this builder
         * @throws NullPointerException if {@code runIdSupplier} is null
         */
        public Builder<C> runIdSupplier(Supplier<String> runIdSupplier) {
            this.runIdSupplier = Objects.requireNonNull(runIdSupplier, "runIdSupplier must not be null");
            return this;
        }

        /**
         * Builds the pipeline with the current configuration.
         *
         * @return a new immutable pipeline
         * @throws IllegalStateException if no step has been added
         */
        public Pipeline<C> build() {
            if (steps.isEmpty()) {
                throw new IllegalStateException("pipeline '" + name + "' must have at least one step");
            }
            return new Pipeline<>(this);
        }
    }
}
