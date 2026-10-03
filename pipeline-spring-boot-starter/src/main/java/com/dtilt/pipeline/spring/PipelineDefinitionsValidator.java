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
 * Controlla all'avvio, dopo la creazione di tutti i bean singleton, che la configurazione sia
 * coerente con le pipeline registrate come bean:
 *
 * <ul>
 *   <li>due bean {@link Pipeline} non possono avere lo stesso nome (sempre bloccante);</li>
 *   <li>ogni voce di {@code pipeline.disabled-steps} deve riferirsi a una pipeline e a uno step
 *       esistenti (bloccante se {@code pipeline.fail-on-unknown-steps=true}, altrimenti WARN).</li>
 * </ul>
 *
 * <p>Sono validate solo le pipeline registrate come bean: una pipeline costruita dentro un
 * costruttore o un metodo non è visibile al validatore.
 */
public class PipelineDefinitionsValidator implements SmartInitializingSingleton {

    private static final Log logger = LogFactory.getLog(PipelineDefinitionsValidator.class);

    private final ListableBeanFactory beanFactory;
    private final PipelineProperties properties;

    /**
     * Crea il validatore.
     *
     * @param beanFactory bean factory da cui leggere i bean {@link Pipeline}
     * @param properties  configurazione delle pipeline
     */
    public PipelineDefinitionsValidator(ListableBeanFactory beanFactory, PipelineProperties properties) {
        this.beanFactory = Objects.requireNonNull(beanFactory, "beanFactory must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * Esegue la validazione, raccogliendo tutti i problemi prima di segnalarli.
     *
     * @throws PipelineConfigurationException se una voce di {@code disabled-steps} è malformata,
     *                                        se esistono nomi di pipeline duplicati, oppure se
     *                                        esistono voci sconosciute e
     *                                        {@code fail-on-unknown-steps} è {@code true}
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
