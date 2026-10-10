package com.dtilt.pipeline.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dtilt.pipeline.exception.BusinessRuleException.Category;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BusinessRuleExceptionTest {

    private static BusinessRuleException businessError() {
        return new BusinessRuleException("PRODUCT_NAME_DUPLICATE", Category.CONFLICT, "duplicate name");
    }

    @Test
    void businessRuleExceptionCreatedOutsideAPipelineIsNotLocated() {
        BusinessRuleException error = businessError();

        assertThat(error.getPipeline()).isNull();
        assertThat(error.getStep()).isNull();
        assertThat(error.getCode()).isEqualTo("PRODUCT_NAME_DUPLICATE");
        assertThat(error.getCategory()).isEqualTo(Category.CONFLICT);
        assertThat(error).hasMessage("duplicate name");
    }

    @Test
    void detailsAreEmptyByDefaultAndUnmodifiable() {
        BusinessRuleException withoutDetails = businessError();
        Map<String, Object> source = new HashMap<>(Map.of("name", "widget"));
        BusinessRuleException withDetails =
                new BusinessRuleException("CODE", Category.INVALID_INPUT, "message", source);

        source.put("later", "change");

        assertThat(withoutDetails.getDetails()).isEmpty();
        assertThatThrownBy(() -> withoutDetails.getDetails().put("k", "v"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(withDetails.getDetails()).containsExactly(Map.entry("name", "widget"));
        assertThatThrownBy(() -> withDetails.getDetails().put("k", "v"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void businessRuleExceptionValidatesItsArguments() {
        assertThatThrownBy(() -> new BusinessRuleException(null, Category.CONFLICT, "m"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new BusinessRuleException("  ", Category.CONFLICT, "m"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BusinessRuleException("CODE", null, "m"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new BusinessRuleException("CODE", Category.CONFLICT, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new BusinessRuleException("CODE", Category.CONFLICT, "m", null))
                .isInstanceOf(NullPointerException.class);
    }
}
