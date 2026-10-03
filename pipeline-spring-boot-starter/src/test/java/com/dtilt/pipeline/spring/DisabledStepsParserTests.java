package com.dtilt.pipeline.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DisabledStepsParserTests {

    @Test
    void groupsStepsByPipelineAndStripsWhitespace() {
        Map<String, Set<String>> parsed = DisabledStepsParser.parse(
                List.of("  create-product.date-range ", "update-product.audit", "create-product.audit\t"));

        assertThat(parsed).containsOnlyKeys("create-product", "update-product");
        assertThat(parsed.get("create-product")).containsExactly("date-range", "audit");
        assertThat(parsed.get("update-product")).containsExactly("audit");
    }

    @Test
    void emptyListGivesEmptyMap() {
        assertThat(DisabledStepsParser.parse(List.of())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nodot", "a.b.c", "Create.step", "create.Step", "create_product.step",
            ".step", "pipeline.", "a..b", "a-.b", ""})
    void rejectsMalformedEntries(String entry) {
        assertThatThrownBy(() -> DisabledStepsParser.parse(List.of("valid.entry", entry)))
                .isInstanceOf(PipelineConfigurationException.class)
                .hasMessageContaining("malformed entry '" + entry + "'");
    }

    @Test
    void rejectsNullEntriesAndReportsAllMalformedEntries() {
        assertThatThrownBy(() -> DisabledStepsParser.parse(Arrays.asList("first bad", null, "ok.ok", "SECOND")))
                .isInstanceOf(PipelineConfigurationException.class)
                .hasMessageContaining("'first bad'")
                .hasMessageContaining("''")
                .hasMessageContaining("'SECOND'");
    }
}
