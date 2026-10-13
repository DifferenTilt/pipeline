package io.github.differentilt.pipeline.step;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Decides, for each execution, whether a step is enabled. Disabled steps are skipped.
 *
 * <p>{@link #isEnabled(String, String)} is called on every execution for every step, not when
 * the pipeline is built: an implementation can therefore change its answer at runtime.
 * Implementations must be thread-safe if the pipeline is run by several threads.
 *
 * <p>The activation is not consulted for required steps (see
 * {@code Pipeline.Builder.requiredStep}): those are always executed.
 */
@FunctionalInterface
public interface StepActivation {

    /**
     * Tells whether the step is enabled for the current execution.
     *
     * @param pipeline the pipeline name
     * @param step     the step name
     * @return {@code true} if the step must be executed, {@code false} if it must be skipped
     */
    boolean isEnabled(String pipeline, String step);

    /** All steps always enabled. The default when no activation is configured. */
    StepActivation ALWAYS = (pipeline, step) -> true;

    /**
     * An activation based on a map of pipeline name -&gt; names of the disabled steps.
     * Pipelines or steps missing from the map are enabled. The map is defensively copied.
     *
     * @param disabledStepsByPipeline the disabled steps by pipeline name; not null, without null
     *                                keys or values
     * @return an immutable activation
     */
    static StepActivation disabling(Map<String, Set<String>> disabledStepsByPipeline) {
        Objects.requireNonNull(disabledStepsByPipeline, "disabledStepsByPipeline must not be null");
        Map<String, Set<String>> copy = new HashMap<>();
        disabledStepsByPipeline.forEach((pipeline, steps) -> {
            Objects.requireNonNull(pipeline, "pipeline name must not be null");
            Objects.requireNonNull(steps, "disabled steps of pipeline '" + pipeline + "' must not be null");
            copy.put(pipeline, Set.copyOf(steps));
        });
        return (pipeline, step) -> {
            Set<String> disabled = copy.get(pipeline);
            return disabled == null || !disabled.contains(step);
        };
    }
}
