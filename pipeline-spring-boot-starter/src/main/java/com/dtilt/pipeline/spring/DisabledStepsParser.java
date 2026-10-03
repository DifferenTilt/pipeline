package com.dtilt.pipeline.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Converts the entries of {@code pipeline.disabled-steps}, in the form
 * {@code <pipeline>.<step>}, into the map of pipeline name -> disabled steps expected by
 * {@code StepActivation.disabling}.
 */
final class DisabledStepsParser {

    private static final Pattern ENTRY =
            Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*\\.[a-z0-9]+(-[a-z0-9]+)*$");

    private DisabledStepsParser() {
    }

    /**
     * Validates and converts the entries, removing leading and trailing whitespace.
     *
     * @throws PipelineConfigurationException if one or more entries are malformed; the message
     *                                        lists all of them
     */
    static Map<String, Set<String>> parse(List<String> entries) {
        Map<String, Set<String>> disabledStepsByPipeline = new LinkedHashMap<>();
        List<String> malformed = new ArrayList<>();
        for (String rawEntry : entries) {
            String entry = (rawEntry != null) ? rawEntry.strip() : "";
            if (!ENTRY.matcher(entry).matches()) {
                malformed.add(entry);
                continue;
            }
            int dot = entry.indexOf('.');
            disabledStepsByPipeline
                    .computeIfAbsent(entry.substring(0, dot), pipeline -> new LinkedHashSet<>())
                    .add(entry.substring(dot + 1));
        }
        if (!malformed.isEmpty()) {
            StringBuilder message = new StringBuilder(
                    "Invalid pipeline.disabled-steps: entries must have the form '<pipeline>.<step>' with kebab-case names");
            malformed.forEach(entry -> message.append(System.lineSeparator())
                    .append(" - malformed entry '").append(entry).append("'"));
            throw new PipelineConfigurationException(message.toString());
        }
        return Collections.unmodifiableMap(disabledStepsByPipeline);
    }
}
