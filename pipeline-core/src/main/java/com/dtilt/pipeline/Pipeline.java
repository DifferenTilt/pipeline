package com.dtilt.pipeline;

import com.dtilt.pipeline.event.ListenerErrorPolicy;
import com.dtilt.pipeline.event.PipelineCompleted;
import com.dtilt.pipeline.event.PipelineFailed;
import com.dtilt.pipeline.event.PipelineListener;
import com.dtilt.pipeline.event.PipelineStarted;
import com.dtilt.pipeline.event.StepCompleted;
import com.dtilt.pipeline.event.StepFailed;
import com.dtilt.pipeline.event.StepSkipped;
import com.dtilt.pipeline.event.StepStarted;
import com.dtilt.pipeline.exception.PipelineException;
import com.dtilt.pipeline.exception.StepExecutionException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Sequenza ordinata e immutabile di {@link Step} con un nome, eseguita con {@link #run(Object)}.
 *
 * <p>L'esecuzione è fail-fast: il primo step che lancia un'eccezione interrompe la pipeline e
 * l'eccezione viene propagata al chiamante come {@link PipelineException}.
 *
 * <p>{@code Pipeline} è immutabile e thread-safe: la stessa istanza può eseguire {@code run}
 * concorrentemente su contesti diversi. Il contesto non viene condiviso tra esecuzioni e non deve
 * essere thread-safe.
 *
 * @param <C> tipo del contesto
 */
public final class Pipeline<C> {

    private static final Pattern KEBAB_CASE = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private final String name;
    private final List<NamedStep<C>> steps;
    private final List<String> stepNames;
    private final StepActivation activation;
    private final ListenerChain<C> listeners;
    private final Clock clock;
    private final Supplier<String> runIdSupplier;

    private Pipeline(Builder<C> builder) {
        this.name = builder.name;
        this.steps = List.copyOf(builder.steps);
        this.stepNames = this.steps.stream().map(NamedStep::name).toList();
        this.activation = builder.activation;
        this.listeners = new ListenerChain<>(builder.listeners, builder.listenerErrorPolicy);
        this.clock = builder.clock;
        this.runIdSupplier = builder.runIdSupplier;
    }

    /**
     * Crea un builder per una pipeline con il nome indicato.
     *
     * @param name nome della pipeline, in kebab-case ({@code ^[a-z0-9]+(-[a-z0-9]+)*$})
     * @param <C>  tipo del contesto
     * @return un nuovo builder
     * @throws NullPointerException     se {@code name} è null
     * @throws IllegalArgumentException se {@code name} non è in kebab-case
     */
    public static <C> Builder<C> builder(String name) {
        return new Builder<>(requireKebabCase(name, "pipeline name"));
    }

    /**
     * Nome della pipeline.
     *
     * @return il nome della pipeline
     */
    public String name() {
        return name;
    }

    /**
     * Nomi degli step nell'ordine di esecuzione.
     *
     * @return lista non modificabile dei nomi degli step
     */
    public List<String> stepNames() {
        return stepNames;
    }

    /**
     * Esegue la pipeline: valuta l'attivazione di ogni step, in ordine, ed esegue quelli attivi.
     *
     * @param context contesto dell'esecuzione; non null
     * @return la stessa istanza di contesto ricevuta
     * @throws NullPointerException   se {@code context} è null
     * @throws PipelineException      se uno step lancia una {@code PipelineException} (propagata
     *                                come stessa istanza, localizzata)
     * @throws StepExecutionException se uno step, o la valutazione della sua attivazione, lancia
     *                                un'altra {@link RuntimeException}
     */
    public C run(C context) {
        Objects.requireNonNull(context, "context must not be null");
        String runId = Objects.requireNonNull(runIdSupplier.get(), "runIdSupplier returned null");
        long pipelineStart = System.nanoTime();
        listeners.emit(new PipelineStarted<>(name, runId, clock.instant(), context));

        List<String> executedSteps = new ArrayList<>();
        List<String> skippedSteps = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            NamedStep<C> step = steps.get(i);
            String stepName = step.name();

            boolean enabled;
            try {
                enabled = activation.isEnabled(name, stepName);
            } catch (RuntimeException e) {
                throw fail(runId, context, stepName, i, Duration.ZERO, pipelineStart, e);
            }
            if (!enabled) {
                listeners.emit(new StepSkipped<>(name, runId, clock.instant(), context, stepName, i));
                skippedSteps.add(stepName);
                continue;
            }

            listeners.emit(new StepStarted<>(name, runId, clock.instant(), context, stepName, i));
            long stepStart = System.nanoTime();
            try {
                step.step().execute(context);
            } catch (RuntimeException e) {
                throw fail(runId, context, stepName, i, elapsedSince(stepStart), pipelineStart, e);
            }
            listeners.emit(new StepCompleted<>(name, runId, clock.instant(), context, stepName, i,
                    elapsedSince(stepStart)));
            executedSteps.add(stepName);
        }

        listeners.emit(new PipelineCompleted<>(name, runId, clock.instant(), context,
                elapsedSince(pipelineStart), executedSteps, skippedSteps));
        return context;
    }

    /** Traduce l'eccezione, emette StepFailed e PipelineFailed e restituisce l'eccezione da lanciare. */
    private PipelineException fail(String runId, C context, String stepName, int stepIndex,
                                   Duration stepDuration, long pipelineStart, RuntimeException e) {
        PipelineException error = translate(stepName, e);
        listeners.emit(new StepFailed<>(name, runId, clock.instant(), context, stepName, stepIndex,
                stepDuration, error));
        listeners.emit(new PipelineFailed<>(name, runId, clock.instant(), context,
                elapsedSince(pipelineStart), stepName, error));
        return error;
    }

    private PipelineException translate(String stepName, RuntimeException e) {
        if (e instanceof PipelineException pipelineException) {
            pipelineException.locate(name, stepName);
            return pipelineException;
        }
        return new StepExecutionException(name, stepName, e);
    }

    private static Duration elapsedSince(long startNanos) {
        return Duration.ofNanos(Math.max(0L, System.nanoTime() - startNanos));
    }

    private static String requireKebabCase(String value, String what) {
        Objects.requireNonNull(value, what + " must not be null");
        if (!KEBAB_CASE.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    what + " must be kebab-case (" + KEBAB_CASE.pattern() + "): '" + value + "'");
        }
        return value;
    }

    private record NamedStep<C>(String name, Step<C> step) {
    }

    /**
     * Builder di {@link Pipeline}. Non è thread-safe. Può essere riusato: {@link #build()} copia
     * le liste, quindi le modifiche successive non toccano le pipeline già costruite.
     *
     * @param <C> tipo del contesto
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
         * Aggiunge uno step in coda alla pipeline.
         *
         * @param name nome dello step, in kebab-case e univoco all'interno della pipeline
         * @param step logica dello step; non null
         * @return questo builder
         * @throws NullPointerException     se {@code name} o {@code step} sono null
         * @throws IllegalArgumentException se {@code name} non è in kebab-case o è già usato
         */
        public Builder<C> step(String name, Step<C> step) {
            requireKebabCase(name, "step name");
            Objects.requireNonNull(step, "step must not be null");
            if (steps.stream().anyMatch(existing -> existing.name().equals(name))) {
                throw new IllegalArgumentException(
                        "duplicate step name '" + name + "' in pipeline '" + this.name + "'");
            }
            steps.add(new NamedStep<>(name, step));
            return this;
        }

        /**
         * Imposta l'attivazione degli step. Default: {@link StepActivation#ALWAYS}.
         *
         * @param activation attivazione; non null
         * @return questo builder
         * @throws NullPointerException se {@code activation} è null
         */
        public Builder<C> activation(StepActivation activation) {
            this.activation = Objects.requireNonNull(activation, "activation must not be null");
            return this;
        }

        /**
         * Registra un listener. Può essere chiamato più volte: i listener vengono notificati
         * nell'ordine di registrazione.
         *
         * @param listener listener da registrare; non null
         * @return questo builder
         * @throws NullPointerException se {@code listener} è null
         */
        public Builder<C> listener(PipelineListener<? super C> listener) {
            listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
            return this;
        }

        /**
         * Imposta il comportamento in caso di eccezione lanciata da un listener.
         * Default: {@link ListenerErrorPolicy#LOG_AND_CONTINUE}.
         *
         * @param policy policy da applicare; non null
         * @return questo builder
         * @throws NullPointerException se {@code policy} è null
         */
        public Builder<C> listenerErrorPolicy(ListenerErrorPolicy policy) {
            this.listenerErrorPolicy = Objects.requireNonNull(policy, "listenerErrorPolicy must not be null");
            return this;
        }

        /**
         * Imposta il clock da cui viene letto {@code occurredAt} degli eventi.
         * Default: {@link Clock#systemUTC()}.
         *
         * @param clock clock da usare; non null
         * @return questo builder
         * @throws NullPointerException se {@code clock} è null
         */
        public Builder<C> clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock must not be null");
            return this;
        }

        /**
         * Imposta il generatore degli identificativi di esecuzione. Default: UUID casuale.
         *
         * @param runIdSupplier generatore, invocato una volta per esecuzione; non null e non deve
         *                      restituire null
         * @return questo builder
         * @throws NullPointerException se {@code runIdSupplier} è null
         */
        public Builder<C> runIdSupplier(Supplier<String> runIdSupplier) {
            this.runIdSupplier = Objects.requireNonNull(runIdSupplier, "runIdSupplier must not be null");
            return this;
        }

        /**
         * Costruisce la pipeline con la configurazione corrente.
         *
         * @return una nuova pipeline immutabile
         * @throws IllegalStateException se non è stato aggiunto alcuno step
         */
        public Pipeline<C> build() {
            if (steps.isEmpty()) {
                throw new IllegalStateException("pipeline '" + name + "' must have at least one step");
            }
            return new Pipeline<>(this);
        }
    }
}
