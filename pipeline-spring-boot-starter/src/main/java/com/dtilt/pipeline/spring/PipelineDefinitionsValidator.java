package com.dtilt.pipeline.spring;

import com.dtilt.pipeline.Pipeline;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Checks at startup, once all singleton beans have been created, that the configuration is
 * consistent with the pipelines registered as beans:
 *
 * <ul>
 *   <li>two {@link Pipeline} beans cannot have the same name (always fatal);</li>
 *   <li>every entry of {@code pipeline.disabled-steps} must refer to an existing pipeline and
 *       step (fatal if {@code pipeline.fail-on-unknown-steps=true}, otherwise a WARN).</li>
 * </ul>
 *
 * <p>Only pipelines registered as beans are validated: a pipeline built inside a constructor or
 * a method is not visible to the validator.
 */
public class PipelineDefinitionsValidator implements SmartInitializingSingleton {

    private static final Log logger = LogFactory.getLog(PipelineDefinitionsValidator.class);

    private final ListableBeanFactory beanFactory;
    private final PipelineProperties properties;

    /**
     * Creates the validator.
     *
     * @param beanFactory the bean factory from which the {@link Pipeline} beans are read
     * @param properties  the pipeline configuration
     */
    public PipelineDefinitionsValidator(ListableBeanFactory beanFactory, PipelineProperties properties) {
        this.beanFactory = Objects.requireNonNull(beanFactory, "beanFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * Runs the validation, collecting all problems before reporting them.
     *
     * @throws PipelineConfigurationException if an entry of {@code disabled-steps} is malformed,
     *                                        if there are duplicate pipeline names, or if there
     *                                        are unknown entries and
     *                                        {@code fail-on-unknown-steps} is {@code true}
     */
    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Set<String>> disabledSteps = DisabledStepsParser.parse(properties.getDisabledSteps());
        Map<String, List<Pipeline<?>>> pipelinesByName = new LinkedHashMap<>();
        Map<String, List<String>> beanNamesByPipeline = new LinkedHashMap<>();
        pipelineBeans().forEach((beanName, pipeline) -> {
            pipelinesByName.computeIfAbsent(pipeline.name(), name -> new ArrayList<>()).add(pipeline);
            beanNamesByPipeline.computeIfAbsent(pipeline.name(), name -> new ArrayList<>()).add(beanName);
        });

        List<String> duplicates = new ArrayList<>();
        beanNamesByPipeline.forEach((pipelineName, beanNames) -> {
            if (beanNames.size() > 1) {
                duplicates.add("duplicate pipeline name '" + pipelineName + "' declared by beans " + beanNames);
            }
        });

        List<String> unknown = new ArrayList<>();
        disabledSteps.forEach((pipelineName, steps) -> {
            List<Pipeline<?>> pipelines = pipelinesByName.get(pipelineName);
            for (String step : steps) {
                String entry = "disabled-steps entry '" + pipelineName + "." + step + "': ";
                if (pipelines == null) {
                    unknown.add(entry + "pipeline '" + pipelineName + "' does not exist");
                } else if (pipelines.stream().noneMatch(pipeline -> pipeline.stepNames().contains(step))) {
                    unknown.add(entry + "step '" + step + "' does not exist in pipeline '" + pipelineName + "'");
                }
            }
        });

        if (!duplicates.isEmpty() || (!unknown.isEmpty() && properties.isFailOnUnknownSteps())) {
            StringBuilder message = new StringBuilder("Invalid pipeline configuration:");
            duplicates.forEach(problem -> message.append(System.lineSeparator()).append(" - ").append(problem));
            unknown.forEach(problem -> message.append(System.lineSeparator()).append(" - ").append(problem));
            throw new PipelineConfigurationException(message.toString());
        }
        unknown.forEach(problem -> logger.warn("Ignoring unknown pipeline " + problem));
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Pipeline<?>> pipelineBeans() {
        Map<String, Pipeline<?>> pipelines = new LinkedHashMap<>();
        Map<String, Pipeline> beans = beanFactory.getBeansOfType(Pipeline.class);
        beans.forEach(pipelines::put);
        return pipelines;
    }
}
