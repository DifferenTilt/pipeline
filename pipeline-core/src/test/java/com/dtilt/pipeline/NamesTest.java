package com.dtilt.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NamesTest {

    @ParameterizedTest
    @ValueSource(strings = {"a", "create-product", "step-2", "42"})
    void acceptsKebabCaseAndReturnsTheName(String name) {
        assertThat(Names.requireKebabCase(name, "step name")).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "CreateProduct", "create_product", "create product",
            "-create", "create-", "create--product", "créer"})
    void rejectsBlankOrNonKebabCaseNames(String name) {
        assertThatThrownBy(() -> Names.requireKebabCase(name, "step name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("step name must be kebab-case")
                .hasMessageContaining("'" + name + "'");
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> Names.requireKebabCase(null, "pipeline name"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("pipeline name must not be null");
    }
}
