package com.dtilt.pipeline;

import com.dtilt.pipeline.listener.ListenerErrorPolicy;
import com.dtilt.pipeline.listener.PipelineListener;
import com.dtilt.pipeline.step.StepActivation;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Holds the configuration shared by several pipelines (activation, global listeners, policy
 * and clock) and produces preconfigured {@link Pipeline.Builder}s. Immutable and thread-safe.
 */
public final class PipelineFactory {

    private final StepActivation activation;
    private final List<PipelineListener<Object>> listeners;
    private final ListenerErrorPolicy listenerErrorPolicy;
    private final Clock clock;

    private PipelineFactory(Builder builder) {
        this.activation = builder.activation;
        this.listeners = List.copyOf(builder.listeners);
        this.listenerErrorPolicy = builder.listenerErrorPolicy;
        this.clock = builder.clock;
    }

    /**
     * Creates a factory builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A pipeline builder with activation, global listeners, policy and clock already set.
     * Global listeners are registered first: those added on the returned builder are notified
     * afterwards. The factory settings can still be overridden on the builder.
     *
     * @param name the pipeline name, in kebab-case
     * @param <C>  the context type
     * @return a preconfigured pipeline builder
     * @throws NullPointerException     if {@code name} is null
     * @throws IllegalArgumentException if {@code name} is not kebab-case
     */
    public <C> Pipeline.Builder<C> pipeline(String name) {
        Pipeline.Builder<C> builder = Pipeline.<C>builder(name)
                .activation(activation)
                .listenerErrorPolicy(listenerErrorPolicy)
                .clock(clock);
        listeners.forEach(builder::listener);
        return builder;
    }

    /** Builder for {@link PipelineFactory}. Not thread-safe; it can be reused. */
    public static final class Builder {

        private final List<PipelineListener<Object>> listeners = new ArrayList<>();
        private StepActivation activation = StepActivation.ALWAYS;
        private ListenerErrorPolicy listenerErrorPolicy = ListenerErrorPolicy.LOG_AND_CONTINUE;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        /**
         * Sets the shared activation. Default: {@link StepActivation#ALWAYS}.
         *
         * @param activation the activation; not null
         * @return this builder
         * @throws NullPointerException if {@code activation} is null
         */
        public Builder activation(StepActivation activation) {
            this.activation = Objects.requireNonNull(activation, "activation must not be null");
            return this;
        }

        /**
         * Registers a global listener. It can be called several times: listeners are notified
         * in registration order.
         *
         * @param listener the listener to register; not null
         * @return this builder
         * @throws NullPointerException if {@code listener} is null
         */
        public Builder listener(PipelineListener<Object> listener) {
            listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
            return this;
        }

        /**
         * Sets the shared policy for listener exceptions.
         * Default: {@link ListenerErrorPolicy#LOG_AND_CONTINUE}.
         *
         * @param policy the policy to apply; not null
         * @return this builder
         * @throws NullPointerException if {@code policy} is null
         */
        public Builder listenerErrorPolicy(ListenerErrorPolicy policy) {
            this.listenerErrorPolicy = Objects.requireNonNull(policy, "listenerErrorPolicy must not be null");
            return this;
        }

        /**
         * Sets the shared clock. Default: {@link Clock#systemUTC()}.
         *
         * @param clock the clock to use; not null
         * @return this builder
         * @throws NullPointerException if {@code clock} is null
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
            return this;
        }

        /**
         * Builds the factory with the current configuration.
         *
         * @return a new immutable factory
         */
        public PipelineFactory build() {
            return new PipelineFactory(this);
        }
    }
}
