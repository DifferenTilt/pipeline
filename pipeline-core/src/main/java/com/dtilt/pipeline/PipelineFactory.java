package com.dtilt.pipeline;

import com.dtilt.pipeline.listener.ListenerErrorPolicy;
import com.dtilt.pipeline.listener.PipelineListener;
import com.dtilt.pipeline.step.StepActivation;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contiene la configurazione condivisa tra più pipeline (attivazione, listener globali, policy
 * e clock) e produce {@link Pipeline.Builder} già configurati. Immutabile e thread-safe.
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
     * Crea un builder di factory.
     *
     * @return un nuovo builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder di pipeline con attivazione, listener globali, policy e clock già impostati.
     * I listener globali sono registrati per primi: quelli aggiunti sul builder restituito
     * vengono notificati dopo. Le impostazioni della factory restano sovrascrivibili sul builder.
     *
     * @param name nome della pipeline, in kebab-case
     * @param <C>  tipo del contesto
     * @return un builder di pipeline preconfigurato
     * @throws NullPointerException     se {@code name} è null
     * @throws IllegalArgumentException se {@code name} non è in kebab-case
     */
    public <C> Pipeline.Builder<C> pipeline(String name) {
        Pipeline.Builder<C> builder = Pipeline.<C>builder(name)
                .activation(activation)
                .listenerErrorPolicy(listenerErrorPolicy)
                .clock(clock);
        listeners.forEach(builder::listener);
        return builder;
    }

    /** Builder di {@link PipelineFactory}. Non è thread-safe; può essere riusato. */
    public static final class Builder {

        private final List<PipelineListener<Object>> listeners = new ArrayList<>();
        private StepActivation activation = StepActivation.ALWAYS;
        private ListenerErrorPolicy listenerErrorPolicy = ListenerErrorPolicy.LOG_AND_CONTINUE;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        /**
         * Imposta l'attivazione condivisa. Default: {@link StepActivation#ALWAYS}.
         *
         * @param activation attivazione; non null
         * @return questo builder
         * @throws NullPointerException se {@code activation} è null
         */
        public Builder activation(StepActivation activation) {
            this.activation = Objects.requireNonNull(activation, "activation must not be null");
            return this;
        }

        /**
         * Registra un listener globale. Può essere chiamato più volte: i listener vengono
         * notificati nell'ordine di registrazione.
         *
         * @param listener listener da registrare; non null
         * @return questo builder
         * @throws NullPointerException se {@code listener} è null
         */
        public Builder listener(PipelineListener<Object> listener) {
            listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
            return this;
        }

        /**
         * Imposta la policy condivisa per le eccezioni dei listener.
         * Default: {@link ListenerErrorPolicy#LOG_AND_CONTINUE}.
         *
         * @param policy policy da applicare; non null
         * @return questo builder
         * @throws NullPointerException se {@code policy} è null
         */
        public Builder listenerErrorPolicy(ListenerErrorPolicy policy) {
            this.listenerErrorPolicy = Objects.requireNonNull(policy, "listenerErrorPolicy must not be null");
            return this;
        }

        /**
         * Imposta il clock condiviso. Default: {@link Clock#systemUTC()}.
         *
         * @param clock clock da usare; non null
         * @return questo builder
         * @throws NullPointerException se {@code clock} è null
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
            return this;
        }

        /**
         * Costruisce la factory con la configurazione corrente.
         *
         * @return una nuova factory immutabile
         */
        public PipelineFactory build() {
            return new PipelineFactory(this);
        }
    }
}
